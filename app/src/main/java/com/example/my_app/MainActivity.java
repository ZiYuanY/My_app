package com.example.my_app;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import Function.authentication;
import database.Database;

/*此为测试主体功能activity，功能并未写完，仅测试的部分功能*/
public class MainActivity extends AppCompatActivity {
    static final int id_num=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                //获取设备AndroidID
                String id= authentication.getDeviceId(MainActivity.this);
                //提示
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "设备id为："+id, Toast.LENGTH_SHORT).show();
                    }
                });

                //生成音频指纹
                float[] fingerprint=authentication.Audio_fingerprint_generate(authentication.padToPowerOfTwo(authentication.idToAudioData(id)));

                //创建数据库实例
                Database database=new Database(MainActivity.this);

                //清空数据库
                database.clearDatabase();

                //检测是否插入成功
                boolean insert=database.insertData(id_num,fingerprint);
                //提示
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "插入是否成功：" + insert, Toast.LENGTH_SHORT).show();
                    }
                });

                //从数据库内拿出数据
                float[] localArray=database.getData(id_num);
                boolean MA_result=authentication.matchArrays(fingerprint,localArray);
                //提示
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "验证是否通过：" + MA_result, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();

    }
}