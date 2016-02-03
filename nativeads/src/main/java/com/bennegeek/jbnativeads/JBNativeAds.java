package com.bennegeek.olxnativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bennegeek.jbnativeads.JBAdsCache;
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
    public int maximumAdsCached = 20;
    public int keywordPrefetchCount = 3;

    private
    String mUnitId;
    private
    Context mContext;

    AdLoader.Builder mLoaderBuilder;

    JBAdsCache mCache = new JBAdsCache(maximumAdsCached);

    // Gets a cached ad that has not expired yet
    public void getAd(final String keyword, final JBNativeAdListener listener) {
        NativeAd ad = mCache.pop(keyword);
        if (ad != null) {
            listener.didGetAd(ad);

            fillCache(keyword);
        } else {
            loadAd(keyword, new JBNativeAdsLoaderListener() {
                @Override
                public void didLoadAd(NativeAd ad) {
                    getAd(keyword,listener);
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
                        .setReturnUrlsForImageAssets(true)
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build());
    }

    // Private Methods

    private interface JBNativeAdsLoaderListener {
        void didLoadAd(NativeAd ad);
    }

    private void cacheNativeAd(NativeAd ad, String keyword) {
        mCache.add(ad,keyword);
    }

    private void fillCache(String keyword) {
        int cacheSize = mCache.getKeywordCount(keyword);

        if (cacheSize < keywordPrefetchCount) {
            loadAd(keyword,null);
        }
    }

    private void loadAd(final String keyword, final JBNativeAdsLoaderListener listener) {
        if (listener != null) {
            mLoaderBuilder
                    .forAppInstallAd(new NativeAppInstallAd.OnAppInstallAdLoadedListener() {
                        @Override
                        public void onAppInstallAdLoaded(NativeAppInstallAd appInstallAd) {
                            // Show the app install ad.
                            cacheNativeAd(appInstallAd, keyword);
                            listener.didLoadAd(appInstallAd);

                            fillCache(keyword);
                        }
                    })
                    .forContentAd(new NativeContentAd.OnContentAdLoadedListener() {
                        @Override
                        public void onContentAdLoaded(NativeContentAd contentAd) {
                            // Show the content ad.
                            cacheNativeAd(contentAd, keyword);
                            listener.didLoadAd(contentAd);

                            fillCache(keyword);
                        }
                    });
        }
        mLoaderBuilder.build().loadAd(new AdRequest.Builder().build());
    }
}
