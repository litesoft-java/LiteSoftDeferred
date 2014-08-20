package org.litesoft.deferred.client;

import org.litesoft.commonfoundation.exceptions.*;
import org.litesoft.deferred.shared.*;

import com.google.gwt.user.client.*;

public class GWTDeferredRunnerFactoryImpl extends DeferredRunnerFactory {
    public GWTDeferredRunnerFactoryImpl( ExceptionOccurredCallback pExceptionOccurredCallback ) {
        super( pExceptionOccurredCallback );
    }

    @Override
    public DeferredRunner create( Runnable pRunnable ) {
        return new OurDeferredRunner( pRunnable );
    }

    private class OurDeferredRunner extends Timer implements DeferredRunner {
        private Runnable mRunnable;

        private OurDeferredRunner( Runnable pRunnable ) {
            mRunnable = pRunnable;
        }

        @Override
        public void run() {
            try {
                mRunnable.run();
            }
            catch ( Exception e ) {
                handleUnexpected( e );
            }
        }
    }
}
