package com.bennegeek.olxnativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;

import java.util.ArrayList;

/**
 * Created by JB on 2/2/16.
 *
 * Simple loader for AdMob Native Ads
 * Handles prefetching and invalidating old prefetched items
 */

interface JBNativeAdListener {
    void didGetAd(NativeAd ad);
}

public class JBNativeAds {
    public static final long AdExpirationMillis = 60 * 60 * 1000; // 1 hour
    public int maximumAdsCached = 4;

    private
    String mUnitId;
    private
    Context mContext;

    AdLoader.Builder mLoaderBuilder;

    JBAdsCache mCache = new JBAdsCache(maximumAdsCached);

    // Gets a cached ad that has not expired yet
    public void getAd(final JBNativeAdListener listener) {
        NativeAd ad = mCache.pop();
        if (ad != null) {
            listener.didGetAd(ad);

            fillCache();
        } else {
            loadAd(new JBNativeAdsLoaderListener() {
                @Override
                public void didLoadAd(NativeAd ad) {
                    getAd(listener);
                }
            });
        }
    }

    public JBNativeAds(Context context, String unitId) {
        mContext = context;
        mUnitId = unitId;

        mLoaderBuilder = new AdLoader.Builder(mContext, mUnitId)
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // Handle the failure by logging, altering the UI, etc.
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build());

        fillCache();
    }

    // Private Methods

    private interface JBNativeAdsLoaderListener {
        void didLoadAd(NativeAd ad);
    }

    private class JBAdsCache {
        public int maximumAdsCached;

        ArrayList<NativeAdWithTimestamp> mAdsCache = new ArrayList<NativeAdWithTimestamp>();

        JBAdsCache(int max) {
            maximumAdsCached = max;
        }

        public void add(NativeAd ad) {
            synchronized (mAdsCache) {
                mAdsCache.add(new NativeAdWithTimestamp(ad, System.currentTimeMillis()));
            }
        }

        public NativeAd pop() {
            synchronized (mAdsCache) {
                if (mAdsCache.size() > 0) {
                    NativeAdWithTimestamp ad = mAdsCache.get(0);

                    mAdsCache.remove(0);

                    long current = System.currentTimeMillis();
                    if (current - ad.timestamp >= AdExpirationMillis) {
                        return null;
                    }

                    return ad.ad;
                }

                return null;
            }
        }

        public int size() {
            synchronized (mAdsCache) {
                return mAdsCache.size();
            }
        }

    }

    private class NativeAdWithTimestamp {
        NativeAd ad;
        long timestamp;

        NativeAdWithTimestamp(NativeAd a, long t) {
            ad = a;
            timestamp = t;
        }
    }

    private void cacheNativeAd(NativeAd ad) {
        mCache.add(ad);
    }

    private void fillCache() {
        int cacheSize = mCache.size();

        if (cacheSize < maximumAdsCached) {
            loadAd(null);
        }
    }

    private void loadAd(final JBNativeAdsLoaderListener listener) {
        if (listener != null) {
            mLoaderBuilder
                    .forAppInstallAd(new NativeAppInstallAd.OnAppInstallAdLoadedListener() {
                        @Override
                        public void onAppInstallAdLoaded(NativeAppInstallAd appInstallAd) {
                            // Show the app install ad.
                            cacheNativeAd(appInstallAd);
                            listener.didLoadAd(appInstallAd);

                            fillCache();
                        }
                    })
                    .forContentAd(new NativeContentAd.OnContentAdLoadedListener() {
                        @Override
                        public void onContentAdLoaded(NativeContentAd contentAd) {
                            // Show the content ad.
                            cacheNativeAd(contentAd);
                            listener.didLoadAd(contentAd);

                            fillCache();
                        }
                    });
        }
        mLoaderBuilder.build().loadAd(new AdRequest.Builder().build());
    }
}
