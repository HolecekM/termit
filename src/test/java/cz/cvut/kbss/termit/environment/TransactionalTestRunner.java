/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.environment;

import cz.cvut.kbss.jopa.model.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfigureDataJpa
@AutoConfigurationPackage(basePackageClasses = {cz.cvut.kbss.termit.config.AppConfig.class})
public abstract class TransactionalTestRunner {

    @Autowired
    protected PlatformTransactionManager txManager;

    @Autowired
    @Qualifier("jpaTxManager")
    protected PlatformTransactionManager jpaTxManager;

    protected void transactional(Runnable procedure) {
        Transaction.execute(txManager, procedure);
    }

    protected void jpaTransactional(Runnable runnable) {
        Transaction.execute(jpaTxManager, runnable);
    }

    protected void enableRdfsInference(EntityManager em) {
        transactional(() -> Environment.addModelStructureForRdfsInference(em));
    }
}
