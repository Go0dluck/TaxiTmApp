#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_order_taxitmapp1_SettingsServer_getNativeKeyApiKey(JNIEnv *env, jobject instance) {

    return (*env)->NewStringUTF(env, "MjNvbGk7aGZnaGRmYXNsaWt2Z2hma2xqc3ZnaGdoZg==");
}

JNIEXPORT jstring JNICALL
Java_com_order_taxitmapp1_SettingsServer_getNativeKeyCallKey(JNIEnv *env, jobject instance) {

    return (*env)->NewStringUTF(env, "MjNmZGh4ZHRmc2hqZmpoZmpoa2pqLGxoa2csbA==");
}//
// Created by Dmitriy on 17.05.2020.
//

