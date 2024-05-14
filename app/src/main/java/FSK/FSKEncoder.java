package FSK;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
//来源网络
public class FSKEncoder {
    public interface FSKEncoderCallback {
        public void encoded(byte[] pcm8, short[] pcm16);
    }
    protected Runnable mProcessor = new Runnable() {
        @Override
        public void run() {
            while (mRunning) {
                synchronized (mData) {
                    switch (mEncoderStatus) {
                        case IDLE:
                            stop();
                            break;
                        case PRE_CARRIER:case POST_CARRIER:
                            processIterationCarrier();
                            break;
                        case ENCODING:
                            processIterationEncoding();
                            break;
                        case SILENCE:
                            processIterationSilence();
                            break;
                    }
                }
            }
        }
    };
    protected enum STATE {
        HIGH, LOW, SILENCE
    }
    protected enum EncoderStatus {
        IDLE, PRE_CARRIER, ENCODING, POST_CARRIER, SILENCE
    }
    protected FSKConfig mConfig;
    protected FSKEncoderCallback mCallback;
    protected Thread mThread;
    protected boolean mRunning = false;
    protected EncoderStatus mEncoderStatus = EncoderStatus.IDLE;
    protected ShortBuffer mSignalPCM16;
    protected ByteBuffer mSignalPCM8;
    protected int mSignalLength = 0;
    protected ByteBuffer mData;
    protected int mDataLength = 0;
    protected int mDataPointer = 0;
    protected int mPreCarrierBits = 0;
    protected int mPostCarrierBits = 0;
    protected int mSilenceBits = 0;
    public FSKEncoder(FSKConfig config, FSKEncoderCallback callback) {
        mConfig = config;
        mCallback = callback;
        mPreCarrierBits = FSKConfig.ENCODER_PRE_CARRIER_BITS; //(int) Math.ceil(mConfig.sampleRate * 40.0f / 1000.0f / mConfig.samplesPerBit);
        mPostCarrierBits = FSKConfig.ENCODER_POST_CARRIER_BITS; //(int) Math.ceil(mConfig.sampleRate * 5.0f / 1000.0f / mConfig.samplesPerBit);
        mSilenceBits = FSKConfig.ENCODER_SILENCE_BITS;//(int) Math.ceil(mConfig.sampleRate * 5.0f / 1000.0f / mConfig.samplesPerBit);
        allocateBufferSignal();
        allocateBufferData();
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }
    protected void notifyCallback(byte[] pcm8, short[] pcm16) {
        if (mCallback != null) {
            mCallback.encoded(pcm8, pcm16);
        }
    }
    protected void start() {
        if (!mRunning) {
            setStatus(EncoderStatus.PRE_CARRIER);
            mRunning = true;
            mThread = new Thread(mProcessor);
            mThread.setPriority(Thread.MIN_PRIORITY);
            mThread.start();
        }
    }
    public void stop() {
        if (mRunning) {
            if (mThread != null && mThread.isAlive()) {
                mRunning = false;

                mThread.interrupt();
            }
        }
    }
    protected void nextStatus() {
        switch (mEncoderStatus) {
            case IDLE:
                setStatus(EncoderStatus.PRE_CARRIER);
                break;
            case PRE_CARRIER:
                setStatus(EncoderStatus.ENCODING);
                break;
            case ENCODING:
                setStatus(EncoderStatus.POST_CARRIER);
                break;
            case POST_CARRIER:
                setStatus(EncoderStatus.SILENCE);
                break;
            case SILENCE:
                setStatus(EncoderStatus.IDLE);
                break;
        }
    }
    protected void setStatus(EncoderStatus status) {
        mEncoderStatus = status;
    }
    protected void allocateBufferSignal() {
        if (mConfig.pcmFormat == FSKConfig.PCM_8BIT) {
            mSignalPCM8 = ByteBuffer.allocate(mConfig.sampleRate); //1 second buffer
        }
        else if (mConfig.pcmFormat == FSKConfig.PCM_16BIT) {
            mSignalPCM16 = ShortBuffer.allocate(mConfig.sampleRate); //1 second buffer
        }
    }
    protected void allocateBufferData() {
        mData = ByteBuffer.allocate(FSKConfig.ENCODER_DATA_BUFFER_SIZE);
    }
    protected void trimData() {
        if (mDataPointer <= mDataLength) {
            byte[] currentData = mData.array();
            byte[] remainingData = new byte[mDataLength - mDataPointer];
            for (int i = 0; i < remainingData.length; i++) {
                remainingData[i] = currentData[mDataPointer+i];
            }
            mData = ByteBuffer.allocate(FSKConfig.ENCODER_DATA_BUFFER_SIZE);
            mData.put(remainingData);
            mData.rewind();
            mDataPointer = 0;
            mDataLength = remainingData.length;
        }
        else {
            clearData();
        }
    }
    public int appendData(byte[] data) {
        synchronized (mData) {
            if (mDataLength + data.length > mData.capacity()) {
                if ((mDataLength + data.length)-mDataPointer <= mData.capacity()) {
                    trimData();
                }
                else {
                    return (mData.capacity() - (mDataLength + data.length));
                }
            }
            mData.position(mDataLength);
            mData.put(data);
            mDataLength += data.length;
            start(); //if idle
            return (mData.capacity() - mDataLength);
        }
    }
    public int setData(byte[] data) {
        allocateBufferSignal();
        clearData();
        return appendData(data);
    }
    public int clearData() {
        synchronized (mData) {
            allocateBufferSignal();
            mDataLength = 0;
            mDataPointer = 0;
            return mData.capacity();
        }
    }
    protected void flushSignal() {
        if (mSignalLength > 0) {
            if (mConfig.pcmFormat == FSKConfig.PCM_8BIT) {
                byte[] dataPCM8 = new byte[mSignalLength];
                for (int i = 0; i < mSignalLength; i++) {
                    dataPCM8[i] = mSignalPCM8.get(i);
                }
                notifyCallback(dataPCM8, null);
            }
            else if (mConfig.pcmFormat == FSKConfig.PCM_16BIT) {
                short[] dataPCM16 = new short[mSignalLength];
                for (int i = 0; i < mSignalLength; i++) {
                    dataPCM16[i] = mSignalPCM16.get(i);
                }
                notifyCallback(null, dataPCM16);
            }
            mSignalLength = 0;
            allocateBufferSignal();
        }
    }
    protected void checkSignalBuffer() {
        if (mConfig.pcmFormat == FSKConfig.PCM_8BIT) {
            if (mSignalLength >= mSignalPCM8.capacity() - 2) {
                flushSignal();
            }
        }
        else {
            if (mSignalLength >= mSignalPCM16.capacity() - 2) {
                flushSignal();
            }
        }
    }
    protected void modulate(STATE state) {
        if (!state.equals(STATE.SILENCE)) {
            if (mConfig.pcmFormat == FSKConfig.PCM_8BIT) {
                byte[] newData = modulate8(state);
                mSignalPCM8.position(mSignalLength);
                for (int i = 0; i < mConfig.samplesPerBit; i++) {
                    mSignalPCM8.put(newData[i]);
                    mSignalLength++;
                    if (mConfig.channels == FSKConfig.CHANNELS_STEREO) {
                        mSignalPCM8.put(newData[i]);
                        mSignalLength++;
                    }
                    checkSignalBuffer();
                }
            }
            else if (mConfig.pcmFormat == FSKConfig.PCM_16BIT) {
                short[] newData = modulate16(state);
                for (int i = 0; i < mConfig.samplesPerBit; i++) {
                    mSignalPCM16.put(newData[i]);
                    mSignalLength++;
                    if (mConfig.channels == FSKConfig.CHANNELS_STEREO) {
                        mSignalPCM16.put(newData[i]);
                        mSignalLength++;
                    }
                    checkSignalBuffer();
                }
            }
        }
        else {
            if (mConfig.pcmFormat == FSKConfig.PCM_8BIT) {
                mSignalPCM8.position(mSignalLength);
                for (int i = 0; i < mConfig.samplesPerBit; i++) {
                    mSignalPCM8.put((byte) 0);
                    mSignalLength++;
                    if (mConfig.channels == FSKConfig.CHANNELS_STEREO) {
                        mSignalPCM8.put((byte) 0);
                        mSignalLength++;
                    }
                    checkSignalBuffer();
                }
            }
            else if (mConfig.pcmFormat == FSKConfig.PCM_16BIT) {
                mSignalPCM16.position(mSignalLength);
                for (int i = 0; i < mConfig.samplesPerBit; i++) {
                    mSignalPCM16.put((short) 0);
                    mSignalLength++;
                    if (mConfig.channels == FSKConfig.CHANNELS_STEREO) {
                        mSignalPCM16.put((short) 0);
                        mSignalLength++;
                    }
                    checkSignalBuffer();
                }
            }
        }
    }

    protected byte[] modulate8(STATE state) {
        int freq = 0;
        byte[] buffer = new byte[mConfig.samplesPerBit];
        if (state.equals(STATE.HIGH)) {
            freq = mConfig.modemFreqHigh;
        }
        else {
            freq = mConfig.modemFreqLow;
        }
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (128 + 127 * Math.sin((2 * Math.PI) * (i*1.0f / mConfig.sampleRate) * freq));
        }
        return buffer;
    }
    protected short[] modulate16(STATE state) {
        int freq = 0;
        short[] buffer = new short[mConfig.samplesPerBit];
        if (state.equals(STATE.HIGH)) {
            freq = mConfig.modemFreqHigh;
        }
        else {
            freq = mConfig.modemFreqLow;
        }
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (short) (32767 * Math.sin((2 * Math.PI) * (i*1.0f / mConfig.sampleRate) * freq));
        }
        return buffer;
    }
    protected void processIterationCarrier() {
        if (mEncoderStatus.equals(EncoderStatus.PRE_CARRIER)) {
            for (int i = 0; i < mPreCarrierBits; i++) {
                modulate(STATE.HIGH);
            }
        }
        else if (mEncoderStatus.equals(EncoderStatus.POST_CARRIER)) {
            for (int i = 0; i < mPostCarrierBits; i++) {
                modulate(STATE.HIGH);
            }
        }
        nextStatus();
    }
    protected void processIterationEncoding() {
        if (mDataPointer < mDataLength) {
            mData.position(mDataPointer);
            byte data = mData.get();
            modulate(STATE.LOW);
            for(byte mask = 1; mask != 0; mask <<= 1) {
                if((data & mask) > 0){
                    modulate(STATE.HIGH);
                }
                else{
                    modulate(STATE.LOW);
                }
            }
            modulate(STATE.HIGH); //end bit
            mDataPointer++;
        }
        else {
            nextStatus();
        }
    }
    protected void processIterationSilence() {
        for (int i = 0; i < mSilenceBits; i++) {
            modulate(STATE.SILENCE);
        }
        flushSignal();
        nextStatus();
    }
}
