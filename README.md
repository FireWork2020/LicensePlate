# AndroidOcrLibs 安卓离线识别身份证、驾驶证、银行卡、车牌号
```Xml
1.极速识别二代身份证(无需联网，离线秒扫，极速识别)身份证所有信息, 包含 姓名、性别、出生年月、详细地址，正反面。可识别新疆少数民族身份证，可保存识别图片。
2.极速识别驾驶证（无需联网，离线秒扫，极速识别）国家，初始领证，准驾车型，有效期限，可保存识别图片。
3.极速识别银行卡（无需联网，离线秒扫，极速识别）卡号，过期日期，发卡银行，卡名，机构代码，卡种，可保存识别图片。
4.极速识别车牌号（无需联网，离线秒扫，极速识别）可识别蓝牌，黄牌，黑牌，白牌，绿牌，可获取到车牌号及颜色，可保存识别图片。
```
### 注意事项
```Xml
1.API没有任何限制（如：不要求验签，不限制次数，永久免费，任由您使用）
2.demo中已基本涵盖了所有核心API的使用，代码写的很烂，但您一定看得懂
3.请尽量扫描真实事物，扫描电脑屏幕成功率低且速度慢
4.如发现与其他本地库编译有冲突，请将arr文件中的“armeabi”文件夹改名为"armeabi-v7a",同时删除其他本地库中除了"armeabi-v7a"以外的所有abi文件夹，最后将build.gradle中的abiFilters改为仅有“armeabi-v7a”即可解决冲突
```
### 1.2版-更新日志
```Xml
【2020-2-12 19:00】
1.修复识别车牌号被SO库限制识别次数的bug
2.一定要给INTERNET权限，并且联网情况下使用车牌识别sdk，否则用不了
```

### 鸣谢 XieZhiFa大神
```Xml
（https://github.com/XieZhiFa/IdCardOCR），识别身份证及驾驶证代码提取自该大神的开源项目，已绿化，无需验签。
```

### aar集成方式
```Xml
将文件aar文件复制到 libs目录下, 然后在build.gradle中增加:
```
```Java
   android{
      repositories {
        flatDir {
            dirs 'libs'
        }
      }
      defaultConfig {
        ndk {
            abiFilters "armeabi"
        }
    }
    }
    dependencies {
        implementation (name: 'ocr-library-1.2', ext: 'aar')
    }
```
```Xml
Manifest.xml中给存储读写权限、摄像头权限、internet访问权限
```
### 识别身份证及驾驶证核心api（LibraryInitOCR.class）
```Java
    //1. Application中初始化
    LibraryInitOCR.initOCR(context); 

    //2. 初始化解码器
    /**
     * 解码器初始化, 如果需要保存图片, 需要在调用向系统审核SDCard读写权限.
     * @param context   Activity
     * @param handler   接收解码消息
     * @param isSaveImage   是否保存图片
     */
    public static void initDecode(Context context, Handler handler, boolean isSaveImage)

    //3. 开始解码
    /**
     * 开始解码, 将相机预览数据传递到这里来, onPreviewFrame(byte[] data, Camera camera)
     * @param rect  预览框对象
     * @param previewWidth  界面预览宽
     * @param previewHeight 界面预览高
     * @param data  相机预览数据
     */
    public static void decode(Rect rect, int previewWidth, int previewHeight, byte[] data)

    /**
     * 识别选择的身份证图片(注意提前申请读写权限)
     * @param filePath  文件路径
     */
    public static void decode(String filePath)

    //4.在Activity onDestroy 释放资源
    /**
     * 释放资源
     */
    public static void closeDecode()

    //解码结果通过handler 接收
    switch (msg.what){
        //解码成功
        case LibraryInitOCR.DECODE_SUCCESS: {
            Intent intent = (Intent) msg.obj;
            String result = intent.getStringExtra("OCRResult");
            String headImg = intent.getStringExtra("headImg");
            String fullImg = intent.getStringExtra("fullImg");
            break;
        }

        //解码失败
        case LibraryInitOCR.DECODE_FAIL:{
            break;
        }
    }
```
### 识别银行卡核心api(BankCardAPI.class)
```Java
   /**
     * 解析银行卡信息
     * @param cardNo 卡号 中间不可有空
     * @return $发卡银行$卡名$机构代码$卡种$
     */
    public native String GetBankInfo(String cardNo);

    /**
     * 获取核心版本号
     * @return
     */
    public native String GetKernalVersion();

    /**
     * 识别银行卡
     * @param paramArrayOfByte 拍摄区银行卡图片数据
     * @param paramInt1 拍摄区长度
     * @param paramInt2 拍摄区宽度
     * @param paramArrayOfInt1 用途不明（怀疑是李逵参数）
     * @param paramArrayOfChar1 存放银行卡号
     * @param paramInt3 用途不明（怀疑是李逵参数）
     * @param paramArrayOfInt2 用途不明（怀疑是李逵参数）
     * @param paramArrayOfInt3 存放截取到的银行卡图片
     * @param paramArrayOfChar2 存放有效期截止年月
     * @return 0:成功  其他：失败
     */
    public native int RecognizeNV21(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int[] paramArrayOfInt1, char[] paramArrayOfChar1, int paramInt3, int[] paramArrayOfInt2, int[] paramArrayOfInt3, char[] paramArrayOfChar2);

    /**
     * 用途不明
     */
    public native int SetExpiryDateFlag(int paramInt);
    /**
     * 用途不明
     */
    public native int SetFilterInvalidCard(int paramInt);

    /**
     * 启动识别SDK
     * @param paramString 用途不明（怀疑是李逵参数）
     * @param paramInt 用途不明（怀疑是李逵参数）
     * @return
     */
    public native int WTInitCardKernal(String paramString, int paramInt);

    /**
     * 设置识别区域
     * @param paramArrayOfInt 长度4，分别对应识别区域左、上、右、下坐标
     * @param paramInt1 识别区域长度
     * @param paramInt2 识别区域宽度
     */
    public native void WTSetROI(int[] paramArrayOfInt, int paramInt1, int paramInt2);

    /**
     * 关闭识别sdk
     */
    public native void WTUnInitCardKernal();
```
### 识别车牌号核心api(BankCardAPI.class)
```Java
   /**
     * 初始化识别车牌号sdk
     * @param paramHandler 用于接受识别成功消息
     */
    public NativeOcrPn(Handler paramHandler);
    /**
     * 实时识别车牌号
     * @param paramArrayOfByte1 拍摄区车牌数据
     * @param paramInt1 拍摄区长度
     * @param paramInt2 拍摄区宽度
     * @param paramArrayOfInt 识别区坐标，长度4的数组，分别对应识别区域左、上、右、下坐标
     * @param paramContext 上下文
     * @return
     */
    public int ScanCarNo(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, int[] paramArrayOfInt, Context paramContext);
    /**
     * 获取识别结果
     * @param var1 用于存储返回的识别结果
     * @param var2 var1数组的长度
     * @return
     */
    public native int GetResult(byte[] var1, int var2);
    /**
     * 生成截取的车牌号图片
     * @param var1 存储图片的路径
     * @return
     */
    public native long CarImage(byte[] var1);
    
    //解码结果通过handler 接收
    switch (msg.what){
        //解码成功
        case 201: {
            byte[] arrayOfByte = new byte[1024];
            mScanCarApi.GetResult(arrayOfByte, arrayOfByte.length);
            //保存图片路径
            String imagePath = newImgPath();
            mScanCarApi.CarImage(imagePath.getBytes("gbk"));
            JSONObject localJSONObject1 = new JSONObject(new String(arrayOfByte, "gbk"));
            //车牌号
            String mEt_carno = localJSONObject1.getString("Num");
            //行数
            String mEt_layer = localJSONObject1.getString("Layer");
            //颜色
            String mEt_color = localJSONObject1.getString("Color");
        }
    }
```
### 混淆
```Xml
已自动处理混淆，无需处理
```
#### 联系我
```Xml
我的博客：https://blog.csdn.net/qwe112113215
```
```Xml
我的邮箱：810343451@qq.com
```

