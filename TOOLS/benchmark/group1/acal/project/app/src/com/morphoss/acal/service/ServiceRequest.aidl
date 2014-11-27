package com.morphoss.acal.service;

interface ServiceRequest {
	void revertDatabase();
	void saveDatabase();
	void fullResync();
	void discoverHomeSets();
	void updateCollectionsFromHomeSets();

	void homeSetDiscovery(int server);
	void syncCollectionNow(long collectionId);
	void fullCollectionResync(long collectionId);
}  