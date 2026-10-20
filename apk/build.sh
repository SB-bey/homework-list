#!/bin/bash
set -e
SDK=/home/user/android-sdk
BT=$SDK/build-tools/34.0.0
AJ=$SDK/platforms/android-34/android.jar
rm -rf build && mkdir -p build/classes build/dexout

echo "[1/7] aapt2 compile 资源..."
$BT/aapt2 compile --dir res -o build/compiled.zip

echo "[2/7] aapt2 link 打包（含 assets）..."
$BT/aapt2 link -o build/app-unsigned.apk -I $AJ --manifest AndroidManifest.xml -A assets build/compiled.zip

echo "[3/7] javac 编译 Java..."
/home/user/jdk-17.0.20.1+1/bin/javac -source 8 -target 8 -classpath $AJ -d build/classes src/com/homework/list/MainActivity.java

echo "[4/7] d8 转 dex..."
$BT/d8 --release --lib $AJ --output build/dexout build/classes/com/homework/list/*.class

echo "[5/7] 加入 classes.dex..."
cd build/dexout && zip -q ../app-unsigned.apk classes.dex && cd ../..

echo "[6/7] zipalign 对齐..."
$BT/zipalign -f 4 build/app-unsigned.apk build/app-aligned.apk

echo "[7/7] 签名..."
if [ ! -f build/hw.keystore ]; then
  keytool -genkeypair -v -keystore build/hw.keystore -alias hw -keyalg RSA -keysize 2048 -validity 10000 -storepass hw123456 -keypass hw123456 -dname "CN=HomeworkList, OU=HW, O=HW, L=CN, S=CN, C=CN" >/dev/null 2>&1
fi
$BT/apksigner sign --ks build/hw.keystore --ks-pass pass:hw123456 --key-pass pass:hw123456 --out 作业清单.apk build/app-aligned.apk
$BT/apksigner verify 作业清单.apk && echo "✓ 签名验证通过"

echo "=== 完成 ==="
ls -la 作业清单.apk
