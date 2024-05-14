package com.example.my_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*声明：这个activity是我想测试录制音频搞得，没写完，也没试过*/

public class playAudio extends AppCompatActivity {

    public static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 101;
    private AudioRecord audioRecord;
    private int minBufferSize;
    private int sampleRate = 44100;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_audio);

        // 获取Button
        Button recordButton = findViewById(R.id.record_button);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(playAudio.this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startRecording();
                } else {
                    ActivityCompat.requestPermissions(playAudio.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_REQUEST_CODE);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Recording Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecording() {
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat,minBufferSize);
        audioRecord.startRecording();
        isRecording = true;

        Thread recordingThread = new Thread(new Runnable() {
            public void run() {
                readAudioData();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    private void readAudioData() {
        byte[] data = new byte[minBufferSize];
        while (isRecording) {
            audioRecord.read(data, 0, minBufferSize);
            //此数据为接收到音频数据
            float[] floatData = convertByteArrayToFloatArray(data);
            //将这个数据传出去进行匹配（还没写）
        }
    }

    private float[] convertByteArrayToFloatArray(byte[] byteArray) {
        float[] floatArray = new float[byteArray.length / 2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floatArray);
        return floatArray;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }
}