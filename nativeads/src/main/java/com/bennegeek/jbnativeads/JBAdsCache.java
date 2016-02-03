package com.bennegeek.jbnativeads;

import com.google.android.gms.ads.formats.NativeAd;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by JB on 2/2/16.
 */
public class JBAdsCache {
    public static final long AdExpirationMillis = 60 * 60 * 1000; // 1 hour
    public int maximumAdsCached;

    ArrayList<JBAdsCacheEntry> mAdsCache = new ArrayList<JBAdsCacheEntry>();
    HashMap<String,Integer> mKeywordCount = new HashMap<String,Integer>();

    public JBAdsCache(int max) {
        maximumAdsCached = max;
    }

    public void add(NativeAd ad, String keyword) {
        synchronized (mAdsCache) {
            Integer count = mKeywordCount.get(keyword);
            mKeywordCount.put(keyword,++count);

            mAdsCache.add(new JBAdsCacheEntry(ad, System.currentTimeMillis(),keyword));
        }
    }

    public Integer getKeywordCount(String keyword) {
        return mKeywordCount.get(keyword);
    }

    public NativeAd pop(String keyword) {
        synchronized (mAdsCache) {
            int size = mAdsCache.size();
            for (int i = 0; i < size; ++i) {
                JBAdsCacheEntry ad = mAdsCache.get(i);

                long current = System.currentTimeMillis();
                if (current - ad.timestamp >= AdExpirationMillis) {
                    removeAtIndex(keyword, i);
                    --i;
                    --size;

                    continue;
                }

                if (ad.keyword.equals(keyword)) {
                    removeAtIndex(keyword,i);
                    return ad.ad;
                }

                // Different keyword, skip to next
            }
        }

        return null;
    }

    public int size() {
        synchronized (mAdsCache) {
            return mAdsCache.size();
        }
    }

    private void removeAtIndex(String keyword, int i) {
        Integer count = mKeywordCount.get(keyword);
        mKeywordCount.put(keyword, ++count);
        mAdsCache.remove(i);
    }
}