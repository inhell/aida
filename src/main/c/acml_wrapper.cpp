#include "acml.h"
#include "acml_wrapper.h"
#include <stdio.h>

void check_memory(JNIEnv * env, void * arg) {
	if (arg != NULL) {
		return;
	}
	/*
	 * WARNING: Memory leak
	 *pgftnrtl.dll
	 * This doesn't clean up successful allocations prior to throwing this exception.
	 * However, it's a pretty dire situation to be anyway and the client code is not
	 * expected to recover.
	 */
	
	env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "Out of memory transferring array to native code in F2J JNI");
}

// LAPACK

JNIEXPORT void JNICALL Java_ru_inhell_aida_acml_ACML_sgesvd 
	(JNIEnv *env, jobject calling_obj, jstring jobu, jstring jobvt, jint m, jint n, jfloatArray a, jint lda, jfloatArray s, jfloatArray u, jint ldu, jfloatArray vt, jint ldvt, jintArray info)
{	

	char * jni_jobu = (char *) env->GetStringUTFChars(jobu, JNI_FALSE);
	char * jni_jobvt = (char *)env->GetStringUTFChars(jobvt, JNI_FALSE);

	jfloat *jni_a = (jfloat *)env->GetPrimitiveArrayCritical(a, JNI_FALSE);
	check_memory(env, jni_a);

	jfloat *jni_s = (jfloat *)env->GetPrimitiveArrayCritical(s, JNI_FALSE);
	check_memory(env, jni_s);

	jfloat *jni_u = (jfloat *)env->GetPrimitiveArrayCritical(u, JNI_FALSE);
	check_memory(env, jni_u);

	jfloat *jni_vt = (jfloat *)env->GetPrimitiveArrayCritical(vt, JNI_FALSE);
	check_memory(env, jni_vt);

        jint *jni_info = (jint *)env->GetPrimitiveArrayCritical(info, JNI_FALSE);
	check_memory(env, jni_info);

	//MP
	//acmlsetnumthreads(acmlgetnumprocs());

//    env->MonitorEnter(calling_obj);
	sgesvd(jni_jobu[0], jni_jobvt[0], (long)m, (long)n, jni_a, (long)lda, jni_s, jni_u, (long)ldu, jni_vt, (long)ldvt, (int *)&jni_info[0]);
//	env->MonitorExit(calling_obj);

	env->ReleaseStringUTFChars(jobu, jni_jobu);
	env->ReleaseStringUTFChars(jobvt, jni_jobvt);
	env->ReleasePrimitiveArrayCritical(a, jni_a, 0);
	env->ReleasePrimitiveArrayCritical(s, jni_s, 0);
	env->ReleasePrimitiveArrayCritical(u, jni_u, 0);
	env->ReleasePrimitiveArrayCritical(vt, jni_vt, 0);
	env->ReleasePrimitiveArrayCritical(info, jni_info, 0);
}

JNIEXPORT void JNICALL Java_ru_inhell_aida_acml_ACML_sgesdd
	(JNIEnv *env, jobject calling_obj, jstring jobz, jint m, jint n, jfloatArray a, jint lda, jfloatArray s, jfloatArray u, jint ldu, jfloatArray vt, jint ldvt, jintArray info)
{
	char * jni_jobz = (char *) env->GetStringUTFChars(jobz, JNI_FALSE);

	jfloat *jni_a = (jfloat *)env->GetPrimitiveArrayCritical(a, JNI_FALSE);
	check_memory(env, jni_a);

	jfloat *jni_s = (jfloat *)env->GetPrimitiveArrayCritical(s, JNI_FALSE);
	check_memory(env, jni_s);

	jfloat *jni_u = (jfloat *)env->GetPrimitiveArrayCritical(u, JNI_FALSE);
	check_memory(env, jni_u);

	jfloat *jni_vt = (jfloat *)env->GetPrimitiveArrayCritical(vt, JNI_FALSE);
	check_memory(env, jni_vt);

        jint *jni_info = (jint *)env->GetPrimitiveArrayCritical(info, JNI_FALSE);
	check_memory(env, jni_info);

	//MP
	//acmlsetnumthreads(acmlgetnumprocs());

	//env->MonitorEnter(calling_obj);
	sgesdd(jni_jobz[0], (long)m, (long)n, jni_a, (long)lda, jni_s, jni_u, (long)ldu, jni_vt, (long)ldvt, (int *)&jni_info[0]);
	//env->MonitorExit(calling_obj);

	env->ReleaseStringUTFChars(jobz, jni_jobz);
	env->ReleasePrimitiveArrayCritical(a, jni_a, 0);
	env->ReleasePrimitiveArrayCritical(s, jni_s, 0);
	env->ReleasePrimitiveArrayCritical(u, jni_u, 0);
	env->ReleasePrimitiveArrayCritical(vt, jni_vt, 0);
	env->ReleasePrimitiveArrayCritical(info, jni_info, 0);
}


JNIEXPORT void JNICALL Java_ru_inhell_aida_acml_ACML_dgesvd 
	(JNIEnv *env, jobject calling_obj, jstring jobu, jstring jobvt, jint m, jint n, jdoubleArray a, jint lda, jdoubleArray s, jdoubleArray u, jint ldu, jdoubleArray vt, jint ldvt, jintArray info)
{	

	char * jni_jobu = (char *) env->GetStringUTFChars(jobu, JNI_FALSE);
	char * jni_jobvt = (char *)env->GetStringUTFChars(jobvt, JNI_FALSE);

	jdouble *jni_a = (jdouble *)env->GetPrimitiveArrayCritical(a, JNI_FALSE);
	check_memory(env, jni_a);

	jdouble *jni_s = (jdouble *)env->GetPrimitiveArrayCritical(s, JNI_FALSE);
	check_memory(env, jni_s);

	jdouble *jni_u = (jdouble *)env->GetPrimitiveArrayCritical(u, JNI_FALSE);
	check_memory(env, jni_u);

	jdouble *jni_vt = (jdouble *)env->GetPrimitiveArrayCritical(vt, JNI_FALSE);
	check_memory(env, jni_vt);

        jint *jni_info = (jint *)env->GetPrimitiveArrayCritical(info, JNI_FALSE);
	check_memory(env, jni_info);

	//MP
	//acmlsetnumthreads(acmlgetnumprocs());

        //env->MonitorEnter(calling_obj);
	dgesvd(jni_jobu[0], jni_jobvt[0], (long)m, (long)n, jni_a, (long)lda, jni_s, jni_u, (long)ldu, jni_vt, (long)ldvt, (int *)&jni_info[0]);
	//env->MonitorExit(calling_obj);

	env->ReleaseStringUTFChars(jobu, jni_jobu);
	env->ReleaseStringUTFChars(jobvt, jni_jobvt);
	env->ReleasePrimitiveArrayCritical(a, jni_a, 0);
	env->ReleasePrimitiveArrayCritical(s, jni_s, 0);
	env->ReleasePrimitiveArrayCritical(u, jni_u, 0);
	env->ReleasePrimitiveArrayCritical(vt, jni_vt, 0);
	env->ReleasePrimitiveArrayCritical(info, jni_info, 0);
}


//BLAS

JNIEXPORT void JNICALL
Java_ru_inhell_aida_acml_ACML_sgemm 
	(JNIEnv *env, jobject calling_obj, jstring transa, jstring transb, jint m, jint n, jint k, jfloat alpha, jfloatArray a, jint lda, jfloatArray b, jint ldb, jfloat beta, jfloatArray c, jint ldc)
{
    char *jni_transa = (char *)env->GetStringUTFChars(transa, JNI_FALSE);
    char *jni_transb = (char *)env->GetStringUTFChars(transb, JNI_FALSE);

    jfloat *jni_a = (jfloat *)env->GetPrimitiveArrayCritical(a, JNI_FALSE);
	check_memory(env, jni_a);

	jfloat *jni_b = (jfloat *)env->GetPrimitiveArrayCritical(b, JNI_FALSE);
	check_memory(env, jni_b);

	jfloat *jni_c = (jfloat *)env->GetPrimitiveArrayCritical(c, JNI_FALSE);
	check_memory(env, jni_c);

	//MP
	//acmlsetnumthreads(acmlgetnumprocs());

    //env->MonitorEnter(calling_obj);
	sgemm(jni_transa[0], jni_transb[0], (long)m, (long)n, (long)k, alpha, jni_a, (long)lda, jni_b, (long)ldb, beta, jni_c, (long)ldc);
	//env->MonitorExit(calling_obj);

	env->ReleaseStringUTFChars(transa, jni_transa);
	env->ReleaseStringUTFChars(transb, jni_transb);
	env->ReleasePrimitiveArrayCritical(a, jni_a, 0);
	env->ReleasePrimitiveArrayCritical(b, jni_b, 0);
	env->ReleasePrimitiveArrayCritical(c, jni_c, 0);
}

JNIEXPORT void JNICALL
Java_ru_inhell_aida_acml_ACML_dgemm 
	(JNIEnv *env, jobject calling_obj, jstring transa, jstring transb, jint m, jint n, jint k, jdouble alpha, jdoubleArray a, jint lda, jdoubleArray b, jint ldb, jdouble beta, jdoubleArray c, jint ldc)
{
    char *jni_transa = (char *)env->GetStringUTFChars(transa, JNI_FALSE);
    char *jni_transb = (char *)env->GetStringUTFChars(transb, JNI_FALSE);

    jdouble *jni_a = (jdouble *)env->GetPrimitiveArrayCritical(a, JNI_FALSE);
	check_memory(env, jni_a);

	jdouble *jni_b = (jdouble *)env->GetPrimitiveArrayCritical(b, JNI_FALSE);
	check_memory(env, jni_b);

	jdouble *jni_c = (jdouble *)env->GetPrimitiveArrayCritical(c, JNI_FALSE);
	check_memory(env, jni_c);

	//MP
	//acmlsetnumthreads(acmlgetnumprocs());

	dgemm(jni_transa[0], jni_transb[0], (long)m, (long)n, (long)k, alpha, jni_a, (long)lda, jni_b, (long)ldb, beta, jni_c, (long)ldc);

	env->ReleaseStringUTFChars(transa, jni_transa);
	env->ReleaseStringUTFChars(transb, jni_transb);
	env->ReleasePrimitiveArrayCritical(a, jni_a, 0);
	env->ReleasePrimitiveArrayCritical(b, jni_b, 0);
	env->ReleasePrimitiveArrayCritical(c, jni_c, 0);
}

//TEST

JNIEXPORT void JNICALL Java_ru_inhell_aida_acml_ACML_test
	(JNIEnv *env, jobject calling_obj, jstring test)
{
	printf(env->GetStringUTFChars(test, JNI_FALSE));
}


