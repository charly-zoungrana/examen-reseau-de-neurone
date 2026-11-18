package net.zoungrana;

import java.util.logging.Level;
import java.util.logging.Logger;

public aspect AuditAspect {
    private static final Logger LOG = Logger.getLogger("net.zoungrana.audit");

    declare precedence: AuditAspect, SecurityAspect;

    pointcut applicationMethod(): execution(* net.zoungrana..*(..))
        && !within(AuditAspect) && !within(SecurityAspect);

    before(): applicationMethod() {
        LOG.log(Level.FINER, "ENTREE {0}", thisJoinPointStaticPart.getSignature().toShortString());
    }

    after() returning: applicationMethod() {
        LOG.log(Level.FINER, "SUCCES {0}", thisJoinPointStaticPart.getSignature().toShortString());
    }

    after() throwing(Throwable failure): applicationMethod() {
        LOG.log(Level.FINER, "ECHEC {0} ({1})", new Object[] {
            thisJoinPointStaticPart.getSignature().toShortString(),
            failure.getClass().getSimpleName()
        });
    }
}
