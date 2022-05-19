/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.RecentlyModifiedAsset;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.TestChangeVectorPersistService;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class AssetDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TestChangeVectorPersistService vectorPersistService;

    @Autowired
    private ResourceDao sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
    }

    @Test
    void findRecentlyEditedLoadsSpecifiedCountOfRecentlyEditedResources() {
        enableRdfsInference(em);
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> Generator.generateResourceWithId())
                                                  .collect(Collectors.toList());
        transactional(() -> resources.forEach(em::persist));
        final List<URI> recent = resources.subList(5, resources.size()).stream().map(Resource::getUri)
                                          .collect(Collectors.toList());

        final int count = 3;
        final List<RecentlyModifiedAsset> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(recent.containsAll(result.stream().map(RecentlyModifiedAsset::getUri).collect(Collectors.toList())));
    }

    @Test
    void findRecentlyEditedUsesLastModifiedDateWhenAvailable() {
        enableRdfsInference(em);
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> Generator.generateResourceWithId())
                                                  .collect(Collectors.toList());
        transactional(() -> resources.forEach(em::persist));
        final List<Resource> recent = resources.subList(5, resources.size());
        final List<ChangeVector<?>> updateRecords = recent.stream().map(Generator::generateUpdateChangeVector)
                                                       .collect(Collectors.toList());
        jpaTransactional(() -> vectorPersistService.persistChangeVectors(updateRecords.toArray(ChangeVector[]::new)));
        em.getEntityManagerFactory().getCache().evictAll();

        final int count = 3;
        final List<URI> recentUris = recent.stream().map(Resource::getUri).collect(Collectors.toList());
        final List<RecentlyModifiedAsset> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(recentUris
                .containsAll(result.stream().map(RecentlyModifiedAsset::getUri).collect(Collectors.toList())));
    }

    @Test
    void findRecentlyEditedReturnsAlsoTypeOfChange() {
        enableRdfsInference(em);
        final Resource resource = Generator.generateResourceWithId();
        transactional(() -> em.persist(resource));
        final ChangeVector<?> vector = Generator.generateUpdateChangeVector(resource);
        jpaTransactional(() -> em.persist(vector));

        final int count = 3;
        final List<RecentlyModifiedAsset> result = sut.findLastEdited(count);
        assertFalse(result.isEmpty());
        result.forEach(rma -> assertTrue(rma.getTypes().contains(Vocabulary.s_c_uprava_entity)));
    }

    @Test
    void findRecentlyEditedByUserReturnsAssetsEditedBySpecifiedUser() {
        enableRdfsInference(em);
        final List<Resource> mineResources = IntStream.range(0, 5).mapToObj(i -> Generator.generateResourceWithId())
                                                      .collect(Collectors.toList());
        final List<ChangeVector<?>> persistRecords = mineResources.stream().map(Generator::generateUpdateChangeVector)
                                                                      .collect(Collectors.toList());
        final List<Resource> othersResources = IntStream.range(0, 5).mapToObj(i -> Generator.generateResourceWithId())
                                                        .collect(Collectors.toList());
        final User otherUser = Generator.generateUserWithId();
        transactional(() -> {
            mineResources.forEach(em::persist);
            othersResources.forEach(em::persist);
            em.persist(otherUser);
        });
        final List<ChangeVector<?>> otherPersistRecords = othersResources.stream().map(r -> {
            final ChangeVector<?> rec = Generator.generateUpdateChangeVector(r);
            rec.setAuthorId(otherUser.getUri().toString());
            return rec;
        }).collect(Collectors.toList());

        jpaTransactional(() -> {
            persistRecords.forEach(em::persist);
            otherPersistRecords.forEach(em::persist);
        });

        final List<RecentlyModifiedAsset> result = sut.findLastEditedBy(user, 3);
        assertFalse(result.isEmpty());
        final Set<URI> mineUris = mineResources.stream().map(Resource::getUri).collect(Collectors.toSet());
        assertTrue(
                mineUris.containsAll(result.stream().map(RecentlyModifiedAsset::getUri).collect(Collectors.toList())));
    }
}
