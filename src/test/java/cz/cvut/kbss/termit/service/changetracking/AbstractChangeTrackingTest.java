package cz.cvut.kbss.termit.service.changetracking;

import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.springframework.transaction.annotation.Transactional;

@Transactional("jpaTxManager")
public abstract class AbstractChangeTrackingTest extends BaseServiceTestRunner {
}
