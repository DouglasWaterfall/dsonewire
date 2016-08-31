package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.busmaster.Logger;

public interface HA7SSerial {

    /**
     *
     */
    public enum StartResult {
        /**
         * The serial device has been started or was already started.
         */
        SR_Success,

        /**
         * No port has been specified.
         */
        SR_NoPortName,

        /**
         * The port is already in use.
         */
        SR_Busy,

        /**
         * an internal exception was caught and logged.
         */
        SR_Error

    }

    /**
     * Start the serial connector.
     *
     * @return 0 if successful, negative values for errors
     */
    public StartResult start(Logger optLogger);

    /**
     * The HA7S uses a write/read sequence to communicate with the host and this method will write bytes (terminated
     * by the caller with a CR as appropriate) and then wait for a response which will always be terminated by the a CR.
     *
     * @param wBuf         array of byte buffers to write from
     * @param rBuf         byte buffer to place read bytes into, expected length, including space for CR is what is expected to
     *                     be read.
     * @param rTimeoutMSec positive time to wait for a read reply
     * @return ReadResult
     */
    public ReadResult writeReadTilCR(byte wBuf[], byte rBuf[], long rTimeoutMSec, Logger optLogger);

    class ReadResult {
        public ReadResult() {
            error = null;
            readCount = 0;
            postWriteCTM = null;
        }

        public enum ErrorCode {
            /**
             * The expected number of bytes were read.
             */
            RR_Success,

            /**
             * Too many bytes to fill the size of rbuf. readCount is the total number of bytes up to and including the
             * CR, but only rbuf.length bytes will be returned.
             */
            RR_ReadOverrun,

            /**
             * The read did not return enough characters in the time allowed by the call. One or more bytes may be
             * stored in which case readCount will be advanced.
             */
            RR_ReadTimeout,

            /**
             * An exception was thrown internally. The log will have more information.
             */
            RR_Error

        }

        /**
         * Result of the call. Initialized to null.
         */
        public ErrorCode error;

        /**
         * Number of bytes actually read, NOT including the terminating CR. So if the return returns RR_Success
         * and the readCount was zero then ONLY a CR return was read (and not returned). You cannot read the CR
         * through this API.
         */
        public int readCount;

        /**
         * To be filled in with System.currentTimeMillis() after the final write has completed.
         */
        public Long postWriteCTM; // non null if valid

    }

    /**
     *
     */
    public enum StopResult {
        /**
         * The serial device has been started or was already started.
         */
        SR_Success,

        /**
         * Failed. See log for more information.
         */
        SR_Error

    }

    /**
     * Stop the serial connector.
     *
     * @return StopResult
     */
    public StopResult stop(Logger optLogger);

}
