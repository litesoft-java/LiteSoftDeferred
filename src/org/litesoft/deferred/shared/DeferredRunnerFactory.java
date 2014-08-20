package org.litesoft.deferred.shared;

import org.litesoft.commonfoundation.base.*;
import org.litesoft.commonfoundation.exceptions.*;

public abstract class DeferredRunnerFactory {
    private static DeferredRunnerFactory sFactory;

    public static synchronized DeferredRunnerFactory getInstance() {
        if ( sFactory == null ) {
            throw new IllegalStateException( "Deferred Runner Factory NOT Initialized!" );
        }
        return sFactory;
    }

    private final ExceptionOccurredCallback mExceptionOccurredCallback;

    protected DeferredRunnerFactory( ExceptionOccurredCallback pExceptionOccurredCallback ) {
        mExceptionOccurredCallback = ConstrainTo.notNull( pExceptionOccurredCallback, ExceptionOccurredCallback.TO_CONSOLE );
        sFactory = this; // Note: leakage == Self Registration!
    }

    abstract public DeferredRunner create( Runnable pRunnable );

    protected void handleUnexpected( Exception pException ) {
        try {
            mExceptionOccurredCallback.exceptionOccurred( this, pException );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
