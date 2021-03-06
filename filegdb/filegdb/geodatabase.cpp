// filegdb_test.cpp : main project file.
#include "Stdafx.h"
#include <vector>
#include <iostream>
#include <string>
#include <jni.h>

using namespace std;
using namespace FileGDBAPI;

extern "C" {

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_initialize(JNIEnv *env, jclass clazz) {
	menv::initialize();
}

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    open
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_test(JNIEnv * env, jobject self) {
	Geodatabase* db = new Geodatabase();
	wchar_t *loc = L"c:/temp/testgdb12345.gdb";
	if (CreateGeodatabase(loc, *db) == S_OK) {
		CloseGeodatabase(*db);
		delete db;
		return (long) 1;
	} else {
		return (long) 0;
	}
};

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    open
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_open(JNIEnv * env, jobject self, jstring path) {
	try {
		menv me(env);
		Geodatabase *geodatabase = new Geodatabase();
		convstr wpath(env, path);
		me.esriCheckedCall(OpenGeodatabase(wpath.getWstr(), *geodatabase), "Creating database failed");
		me.setPtr(self, geodatabase);
	} catch (jni_check) {
        
    }
};

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    closeAndDestroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_closeAndDestroy(JNIEnv * env, jobject self, jstring path) {
	try {
		menv me(env);
		Geodatabase* db = (Geodatabase*) me.getPtr(self, 0L);
		if (db != 0L) {
			me.esriCheckedCall(CloseGeodatabase(*db), "Closing database failed");
		}
		if (path != 0L) {
			convstr wpath(env, path);
			me.esriCheckedCall(DeleteGeodatabase(wpath.getWstr()), "Deleting database failed");
		}
		if (db != 0L) {
			me.setPtr(self, 0L);
			delete db;
		}
	} catch (jni_check) {
		//
    }
};

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    create
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_create(JNIEnv *env, jobject self, jstring path) {
	try {
		menv me(env);
		Geodatabase *db = new Geodatabase();
		convstr wpath(env, path);
		me.esriCheckedCall(CreateGeodatabase(wpath.getWstr(), *db), "Creating database failed");
		me.setPtr(self, db);
	} catch (jni_check) {
		
    }
}

/*
 * Class:     Java_org_opensextant_giscore_filegdb_Geodatabase
 * Method:    getDatasetTypes
 * Signature: ()
 */
JNIEXPORT jobjectArray JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_getDatasetTypes(JNIEnv *env, jobject self) {
	menv me(env);
	Geodatabase* db = me.getGeodatabase(self);
	vector<wstring> datasettypes(3);
	try {
		me.esriCheckedCall(db->GetDatasetTypes(datasettypes), "Problem getting data types");
		return me.processJStringArray(datasettypes);
	} catch (jni_check) {
        return 0;
    }
}

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    getChildDatasets
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_getChildDatasets(JNIEnv *env, jobject self, jstring parent, jstring dataset) {
	menv me(env);
	Geodatabase* db = me.getGeodatabase(self);
	convstr parentPath(env, parent);
	convstr datasetstr(env, dataset);
	vector<wstring> datasetTypes;
	try {
		db->GetChildDatasets(parentPath.getWstr().c_str(), datasetstr.getWstr().c_str(), datasetTypes);
		// Ignore return code, an empty return array will indicate no children found
		return me.processJStringArray(datasetTypes);
	} catch (jni_check) {
        return 0;
    }
}

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    getChildDatasetDefinitions
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_getChildDatasetDefinitions(JNIEnv *env, jobject self, jstring parent, jstring dataset) {
	menv me(env);
	Geodatabase* db = me.getGeodatabase(self);
	convstr parentPath(env, parent);
	convstr datasetstr(env, dataset);
	vector<string> datasetTypes;
	try {
		menv me(env);
		me.esriCheckedCall(db->GetChildDatasetDefinitions(parentPath.getWstr().c_str(), datasetstr.getWstr().c_str(), datasetTypes),
			"Failed to find dataset types");
		return me.processJStringArray2(datasetTypes);
	} catch (jni_check) {
        return 0;
    }
}

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    openTable
 * Signature: (Ljava/lang/String;)Lorg/opensextant/giscore/filegdb/Table;
 */
JNIEXPORT jobject JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_openTable(JNIEnv *env, jobject self, jstring path) {
	Table *table = new Table();
	try {
		menv me(env);
		Geodatabase* db = me.getGeodatabase(self);
		convstr wpath(env, path);
		me.esriCheckedCall(db->OpenTable(wpath.getWstr(), *table), "Opening table failed");
		jclass tc = me.findClass("org.opensextant.giscore.filegdb.Table");
		jobject tobj = env->AllocObject(tc);
		me.setPtr(tobj, table);
		return tobj;
	} catch (jni_check) {
		delete table;
        return 0;
    }	
}

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    createTable
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Lorg/opensextant/giscore/filegdb/Table;
 */
JNIEXPORT jobject JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_createTable(JNIEnv *env, jobject self, jstring parentpath, jstring descriptor) {
	try {
		menv me(env);
		Geodatabase *db = me.getGeodatabase(self);
		Table *t = new Table();
		convstr desc(env, descriptor);
		convstr ppath(env, parentpath);
		me.esriCheckedCall(db->CreateTable(desc.getStr(), ppath.getWstr(), *t), "Create table failed");
		jclass tc = me.findClass("org.opensextant.giscore.filegdb.Table");
		jobject tobj = env->AllocObject(tc);
		me.setPtr(tobj, t);
		return tobj;
	} catch(jni_check) {
		return 0;
	}
}

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    closeTable
 * Signature: (Lorg/opensextant/giscore/filegdb/Table;)V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_closeTable(JNIEnv *env, jobject self, jobject table) {
	try {
		menv me(env);
		Geodatabase *db = me.getGeodatabase(self);
		Table *t = me.getTable(table);
		me.esriCheckedCall(db->CloseTable(*t), "Failed to close table");
	} catch(jni_check) {
		//
	}
}

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    getDatasetDefinition
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_getDatasetDefinition(JNIEnv *env, jobject self, jstring path, jstring type) {
	menv me(env);
	convstr wpath(env, path);
	convstr wtype(env, type);
	string sdatasetDef;
	Geodatabase *db = me.getGeodatabase(self);
	if (db->GetDatasetDefinition(wpath.getWstr(), wtype.getWstr(), sdatasetDef) != S_OK)
		return 0L;
	else
		return env->NewStringUTF(sdatasetDef.c_str());
}

/*
 * Class:     org_opensextant_giscore_filegdb_Geodatabase
 * Method:    createFeatureDataset
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Geodatabase_createFeatureDataset(JNIEnv *env, jobject self, jstring def) {
	try {
		menv me(env);
		convstr wdef(env, def);
		Geodatabase *db = me.getGeodatabase(self);
		me.esriCheckedCall(db->CreateFeatureDataset(wdef.getStr()), "Failed");
	} catch(jni_check) {
		//
	}
}

}
