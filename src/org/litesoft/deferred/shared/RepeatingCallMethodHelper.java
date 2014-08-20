package org.litesoft.deferred.shared;

public class RepeatingCallMethodHelper {
    private static final int DELAY = 100; // 1/10 sec
    private DeferredExecuter mExecuter;

    public RepeatingCallMethodHelper( Runnable pRunnable ) {
        mExecuter = new DeferredExecuter( pRunnable );
    }

    public synchronized void execute() {
        mExecuter.requestExecute();
    }

    private static class DeferredExecuter {
        private Long mRunIn;
        private Long mStartRunAt;
        private DeferredRunner mDeferredRunner;

        public DeferredExecuter( final Runnable pRunnable ) {
            mDeferredRunner = DeferredRunnerFactory.getInstance().create( new Runnable() {
                @Override
                public void run() {
                    while ( shouldRun() ) {
                        try {
                            pRunnable.run();
                        }
                        catch ( RuntimeException e ) {
                            e.printStackTrace();
                        }
                        runCompleted();
                    }
                }
            } );
        }

        public synchronized void requestExecute() {
            boolean zPending = (mRunIn != null);
            mRunIn = System.currentTimeMillis() + DELAY;
            if ( !zPending && (mStartRunAt == null) ) {
                mDeferredRunner.schedule( DELAY );
            }
        }

        private synchronized boolean shouldRun() {
            if ( mRunIn == null ) {
                return false;
            }
            long now = System.currentTimeMillis();
            long milliSecsLeft = mRunIn - now;
            if ( milliSecsLeft > 9 ) {
                mDeferredRunner.schedule( (int) milliSecsLeft );
                return false;
            }
            mStartRunAt = mRunIn;
            mRunIn = null;
            return true;
        }

        private synchronized void runCompleted() {
            // long duration = System.currentTimeMillis() - mStartRunAt;
            mStartRunAt = null;
        }
    }
}
