package Function;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.provider.Settings;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import FFT.ComplexNumberArray;
import FFT.Fourier;

public class authentication {
    /**
     * 此方法用于AndroidID(可选用其他设备唯一标识)
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * 此方法将设备唯一标识符转化时域信号
     * @param id-设备唯一标识符
     * @return-float数组（方便用于FFT）
     */
    public static float[] idToAudioData(String id) {
        // 频率数组（设定的）
        double[] frequencies = new double[]{1575, 1764, 2004, 2321, 2756, 3392, 4410, 6300, 1025, 14700};

        // 创建AudioTrack实例
        int sampleRate = 44100;  // 44100 Hz (CD品质)

        // 初始化数据流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 将16进制字符串id转为10进制字符串
        BigInteger bigInteger = new BigInteger(id, 16);
        String decimalId = bigInteger.toString(10);

        // 遍历每一个字符
        List<Float> bufferList = new ArrayList<>();
        for (char c : decimalId.toCharArray()) {
            // 将字符转换为整数
            int number = Character.getNumericValue(c);

            // 获取对应的频率
            double frequency = frequencies[number];

            // 初始化缓冲区
            float[] buffer = new float[sampleRate]; // 代表1秒声音信号的缓冲区

            // 将数据转化为音频PCM数据
            int sampleDuration = sampleRate; // 每个音频信号的持续时间为 1 秒
            for (int i = 0; i < sampleDuration; i++) {
                buffer[i] = (float) (Math.sin(2 * Math.PI * frequency * i / sampleRate));
            }

            // 添加到数据流
            for (float f : buffer) {
                bufferList.add(f);
            }

            // 在每个音频信号之间，添加0.5秒的静默
            float silence = 0; // 静默的PCM数据就是零
            for (int i = 0; i < sampleRate / 2; i++) {
                bufferList.add(silence);
            }
        }

        // 转换列表为数组并返回
        float[] output = new float[bufferList.size()];
        for (int i = 0; i < bufferList.size(); i++) {
            output[i] = bufferList.get(i);
        }
        return output;
    }

    /**
     * 此方法用于将float数组填充至2的幂次方（FFT需要传入的数组长度必须为2的幂次方）
     * @param inputArray
     * @return
     */
    public static float[] padToPowerOfTwo(float[] inputArray) {
        int originalLength = inputArray.length;

        //检查长度是不是2的幂次方
        if ((originalLength & (originalLength - 1)) == 0) {
            return inputArray;
        }

        //如果不是2的幂次方，则扩充至最近的2的幂次方长度
        int nextPowerOfTwo = (int) Math.pow(2, Math.ceil(Math.log(originalLength) / Math.log(2)));

        //声明返回用的数组
        float[] outputArray = new float[nextPowerOfTwo];

        //用0填充
        System.arraycopy(inputArray, 0, outputArray, 0, originalLength);

        //返回长度为2的幂次方的数组
        return outputArray;
    }

    /**
     * 此方法利用FFT将时域信号转化为频域信号并生成音频指纹
     * @param PCM
     * @return
     */
    public static float[] Audio_fingerprint_generate(float[] PCM){

        //创建FFT实例，并使用FFT
        Fourier fourier = new Fourier(PCM.length, 44100);
        ComplexNumberArray fftResult = fourier.fft(PCM);

        //音频指纹处理，对振幅20log（分贝处理）
        float[] amplitudeArray = fftResult.getAllAmplitude();
        float[] dBAmplitudeArray = new float[amplitudeArray.length];
        for(int i = 0; i < amplitudeArray.length; i++){
            dBAmplitudeArray[i] = 20 * (float)Math.log10(amplitudeArray[i]);
        }

        //返回音频指纹
        return dBAmplitudeArray;
    }

    /**
     * 此方法用于指纹匹配，将需要认证的指纹与学习到的指纹进行匹配
     * @param foreignArray-外来数组，即需要认证的指纹
     * @param localArray-本地数组，即学习到的指纹
     * @return
     */
    public static boolean matchArrays(float[] foreignArray, float[] localArray) {

        //设定的既定阈值，如果超过这个阈值，则匹配不通过
        float threshold1=5;
        float threshold2=5;

        // 确保两个指纹长度一样
        if (foreignArray.length < localArray.length) {
            float[] newForeignArray = new float[localArray.length];
            System.arraycopy(foreignArray, 0, newForeignArray, 0, foreignArray.length);
            foreignArray = newForeignArray;
        } else if (foreignArray.length > localArray.length) {
            float[] newLocalArray = new float[foreignArray.length];
            System.arraycopy(localArray, 0, newLocalArray, 0, localArray.length);
            localArray = newLocalArray;
        }

        int S = 0, T = 0;

        // 迭代两个数组，增加S或者T的值
        for (int i = 0; i < localArray.length; i++) {
            if (Math.abs(foreignArray[i] - localArray[i]) < threshold1) {
                S += 1;
            } else {
                T += 1;
            }
        }

        // 如果T/S小于Threshold2，则输出True，否则输出false
        if (S == 0) {
            return T == 0 && 0 < threshold2;
        } else {
            return ((float) T / S) < threshold2;
        }
    }

    /**
     * 此方法用于播放音频（可直接用idToAudioData方法获得的时域信号播放）（当时测试用的，暂时不知道放哪里就放在这里）
     * @param audioData
     */
    public static void playAudioData(float[] audioData) {
        // 创建AudioTrack实例
        int sampleRate = 44100;  // 44100 Hz (CD品质)
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT, bufferSize, AudioTrack.MODE_STREAM);

        // 播放PCM数据
        audioTrack.play();
        audioTrack.write(audioData, 0, audioData.length, AudioTrack.WRITE_BLOCKING);
    }

}
