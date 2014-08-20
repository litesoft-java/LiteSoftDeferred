package org.litesoft.deferred.server;

import org.litesoft.commonfoundation.base.*;
import org.litesoft.commonfoundation.exceptions.*;
import org.litesoft.deferred.shared.*;

public class ThreadedDeferredRunnerFactoryImpl extends DeferredRunnerFactory {

    /* package Friendly */
    final ThreadedDeferredRunnerManager mManager;

    /* package Friendly */
    ThreadedDeferredRunnerFactoryImpl( ExceptionOccurredCallback pExceptionOccurredCallback, MillisecTimeSource pMillisecTimeSource ) {
        super( pExceptionOccurredCallback );
        mManager = new ThreadedDeferredRunnerManager( MillisecTimeSource.deNull( pMillisecTimeSource ) );
    }

    public ThreadedDeferredRunnerFactoryImpl( ExceptionOccurredCallback pExceptionOccurredCallback ) {
        this( pExceptionOccurredCallback, null );
    }

    @Override
    public DeferredRunner create( Runnable pRunnable ) {
        return new OurDeferredRunner( mManager, pRunnable );
    }

    /* package Friendly */
    class ThreadedDeferredRunnerManager implements Runnable {
        private final MillisecTimeSource mMillisecTimeSource;
        private Thread mMyThread = null;
        private OurDeferredRunner mHead = null; // mHead & mNextRunWhen should be tightly coordinated
        private Long mNextRunWhen = null; // mHead & mNextRunWhen should be tightly coordinated

        /* package Friendly */
        ThreadedDeferredRunnerManager( MillisecTimeSource pMillisecTimeSource ) {
            mMillisecTimeSource = pMillisecTimeSource;
        }

        /* package Friendly */
        synchronized boolean isScheduled() {
            return (mNextRunWhen != null);
        }

        /* package Friendly */
        synchronized void schedule( OurDeferredRunner pDeferredRunner ) {
            reQueue( pDeferredRunner );
            rescheduleHead();
        }

        /* package Friendly */
        synchronized void rescheduleHead() {
            if ( mHead != null ) {
                if ( mMyThread != null ) {
                    notifyAll();
                } else {
                    mMyThread = new Thread( this );
                    mMyThread.setDaemon( true );
                    mMyThread.start();
                }
            }
        }

        private void setHead( OurDeferredRunner pDeferredRunner ) { // mHead & mRunWhen should be tightly coordinated
            mNextRunWhen = ((mHead = pDeferredRunner) == null) ? null : mHead.mRunWhen;
        }

        /* package Friendly */
        synchronized void reQueue( OurDeferredRunner pDeferredRunner ) {
            remove( pDeferredRunner );
            if ( (mHead == null) || (mNextRunWhen == null) || (pDeferredRunner.mRunWhen <= mNextRunWhen) ) {
                pDeferredRunner.mNext = mHead;
                setHead( pDeferredRunner );
                return;
            }
            OurDeferredRunner zAfter = mHead;
            for ( OurDeferredRunner zCur; null != (zCur = zAfter.mNext); zAfter = zCur ) {
                if ( pDeferredRunner.mRunWhen <= zCur.mRunWhen ) {
                    pDeferredRunner.insertBetween( zAfter, zCur );
                    return;
                }
            }
            pDeferredRunner.insertBetween( zAfter, null );
        }

        /* package Friendly */
        synchronized void remove( OurDeferredRunner pDeferredRunner ) {
            if ( pDeferredRunner == mHead ) {
                setHead( pDeferredRunner.mNext );
            }
            pDeferredRunner.remove();
        }

        @Override
        public void run() {
            while ( waitTillTime() ) {
                if ( mHead != null ) {
                    OurDeferredRunner zHead = mHead;
                    remove( mHead );
                    try {
                        zHead.run();
                        zHead.again();
                    }
                    catch ( Exception e ) {
                        handleUnexpected( e );
                    }
                }
            }
        }

        /* package Friendly */
        synchronized boolean waitTillTime() {
            while ( mMyThread != null ) {
                try {
                    while ( mNextRunWhen == null ) {
                        wait();
                    }
                    long zNow = mMillisecTimeSource.now();
                    if ( mNextRunWhen <= zNow ) {
                        mNextRunWhen = null;
                        return true;
                    }
                    long milliSecsToWait = mNextRunWhen - zNow;
                    wait( milliSecsToWait );
                }
                catch ( InterruptedException e ) {
                    // Whatever...
                }
            }
            return false;
        }

        public synchronized void cancel() {
            mNextRunWhen = null;
            if ( mMyThread != null ) {
                notifyAll();
            }
        }
    }

    /* package Friendly */
    static class OurDeferredRunner implements DeferredRunner {
        private final ThreadedDeferredRunnerManager mManager;
        private final Runnable mRunnable;
        private OurDeferredRunner mPrev = null;
        private OurDeferredRunner mNext = null;
        private long mRunWhen; // when scheduled becomes valid
        private Integer mRepeating;

        /* package Friendly */
        OurDeferredRunner( ThreadedDeferredRunnerManager pManager, Runnable pRunnable ) {
            mManager = pManager;
            mRunnable = pRunnable;
        }

        /* package Friendly - called by mManager - NOT locked */
        void run() {
            mRunnable.run();
        }

        /* package Friendly - called by mManager - NOT locked */
        void again() {
            if ( mRepeating != null ) {
                mRunWhen += mRepeating;
                mManager.schedule( this );
            }
        }

        /* package Friendly - run under mManager's lock */
        void remove() {
            if ( this.mPrev != null ) {
                mPrev.mNext = this.mNext;
            }
            if ( this.mNext != null ) {
                mNext.mPrev = this.mPrev;
            }
            this.mPrev = this.mNext = null;
        }

        /* package Friendly - run under mManager's lock */
        void insertBetween( OurDeferredRunner pAfter, OurDeferredRunner pBefore ) { // Assumes we are NOT currently queued!
            if ( pAfter != null ) {
                pAfter.mNext = this;
            }
            if ( pBefore != null ) {
                pBefore.mPrev = this;
            }
            this.mPrev = pAfter;
            this.mNext = pBefore;
        }

        private void scheduleIn( int pDelayMillis, Integer pRepeating ) {
            mRunWhen = mManager.mMillisecTimeSource.now() + pDelayMillis;
            mRepeating = pRepeating;
            mManager.schedule( this );
        }

        /**
         * Schedules the Runnable to "run" on or after the elapsed time.
         *
         * @param pDelayMillis how long to wait before the "run", in
         *                     milliseconds
         */
        @Override
        public void schedule( int pDelayMillis ) {
            scheduleIn( pDelayMillis, null );
        }

        /**
         * Schedules the Runnable to "run" on or after the elapsed time repeatedly.
         *
         * @param pPeriodMillis how long to wait before the "run", in
         *                      milliseconds, initially & between each repetition
         */
        @Override
        public void scheduleRepeating( int pPeriodMillis ) {
            scheduleIn( pPeriodMillis, pPeriodMillis );
        }

        /**
         * Cancels the pending "run"(s).
         */
        @Override
        public void cancel() {
            mManager.remove( this );
        }
    }
}
