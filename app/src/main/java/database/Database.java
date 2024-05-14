package database;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.StringTokenizer;

/*此处为数据类，并未写完，只是能保存、查看和清空数据，重复插入等等问题的解决方法还没写*/

public class Database {

    private DatabaseHelper dbHelper;

    public Database(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * 此方法用于保存指纹到本地
     * @param id-插入的指纹的id数
     * @param dataArray-保存指纹的数组
     * @return
     */
    public boolean insertData(int id, float[] dataArray) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 如果数组太大，分块处理
        final int blockSize = 1000;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < dataArray.length; i++) {
            sb.append(dataArray[i]).append(",");
            if ((i + 1) % blockSize == 0) { //每个blockSize浮动或结束数组
                ContentValues contentValues = new ContentValues();
                contentValues.put(DatabaseHelper.COL_1, id);
                contentValues.put(DatabaseHelper.COL_2, sb.toString()); //保存blockSize元素的字符串

                long result = db.insert(DatabaseHelper.TABLE_NAME, null, contentValues);
                //如果插入失败
                if (result == -1) {
                    db.close();
                    return false;
                }

                sb.setLength(0); //清除StringBuilder
                id++; //增加id
            }
        }

        //如果存在，则处理剩余部分
        if (sb.length() > 0) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.COL_1, id);
            contentValues.put(DatabaseHelper.COL_2, sb.toString());

            long result = db.insert(DatabaseHelper.TABLE_NAME, null, contentValues);
            //如果插入失败
            if (result == -1) {
                db.close();
                return false;
            }
        }

        db.close();
        return true;
    }

    /**
     * 此方法用于查看本地指纹
     * @param id-需要查看的指纹id
     * @return
     */
    public float[] getData(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 从数据库获取字符串
        Cursor cursor = db.rawQuery("select * from " + DatabaseHelper.TABLE_NAME + " where ID = " + id, null);
        if (cursor.moveToFirst()) {

            int columnIndex = cursor.getColumnIndex(DatabaseHelper.COL_2);

            if (columnIndex != -1) {
                String dataString = cursor.getString(columnIndex);

                // 转换为数组
                StringTokenizer st = new StringTokenizer(dataString, ",");
                float[] dataArray = new float[st.countTokens()];
                for (int i = 0; i < dataArray.length; i++) {
                    dataArray[i] = Float.parseFloat(st.nextToken());
                }

                cursor.close();
                return dataArray;
            }
        }

        cursor.close();
        return null;
    }

    /**
     * 此方法用于清空数据库
     */
    public void clearDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //从表中删除所有数据
        String clearDBQuery = "DELETE FROM "+ DatabaseHelper.TABLE_NAME;
        db.execSQL(clearDBQuery);
        db.close();
    }
}
