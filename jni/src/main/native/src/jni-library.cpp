#include <jni.h>
#include <jni_md.h>
#include <jvmti.h>
#include "Main.h" // under target/native/javah/

JNIEXPORT jstring JNICALL Java_Main_helloJni(JNIEnv *env, jclass klass) {
    return env->NewStringUTF("Hello JNI");
}