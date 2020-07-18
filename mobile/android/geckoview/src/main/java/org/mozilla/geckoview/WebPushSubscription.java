/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * vim: ts=4 sw=4 expandtab:
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.geckoview;

import org.mozilla.gecko.util.GeckoBundle;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

/**
 * This class represents a single Web Push subscription, as described in
 * the <a href="https://www.w3.org/TR/push-api/">Web Push API</a> specification.
 *
 * This is a low-level interface, allowing applications to do all of the heavy lifting
 * themselves. It is recommended that consumers have a thorough understanding of the
 * Web Push API, especially <a href="https://tools.ietf.org/html/rfc8291">RFC 8291</a>.
 *
 * Only trivial sanity checks are performed on the values held here. The application must
 * ensure it is generating compliant keys/secrets itself.
 */
public class WebPushSubscription implements Parcelable {
    private static final int P256_PUBLIC_KEY_LENGTH = 65;

    /**
     * The Service Worker scope associated with this subscription.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerContainer/register">ServiceWorker registration</a>
     */
    @NonNull
    public final String scope;

    /**
     * The Web Push endpoint for this subscription. This is the URL of a web service which
     * implements the Web Push protocol.
     *
     * @see <a href="https://tools.ietf.org/html/rfc8030#section-5">RFC 8030</a>
     */
    @NonNull
    public final String endpoint;

    /**
     * This is an optional public key provided by the application server to authenticate
     * itself with the endpoint, formatted according to X9.62.
     *
     * This key is used for VAPID, the Voluntary Application Server Identification (VAPID)
     * for Web Push, from <a href="https://tools.ietf.org/html/rfc8292">RFC 8292</a>.
     *
     * @see <a href="https://www.w3.org/TR/push-api/#dom-pushsubscriptionoptions-applicationserverkey">applicationServerKey</a>
     * @see <a href="https://tools.ietf.org/html/rfc8291">Message Encryption for Web Push</a>
     */
    @Nullable
    public final byte[] appServerKey;

    /**
     * The P-256 EC public key, formatted as X9.62, generated by the embedder, to be provided
     * to the app server for message encryption.
     *
     * @see <a href="https://www.w3.org/TR/push-api/#dom-pushencryptionkeyname-p256dh">PushEncryptionKeyName - p256dh</a>
     * @see <a href="https://tools.ietf.org/html/rfc8291#section-3.1">RFC 8291 section 3.1</a>
     */
    @NonNull
    public final byte[] browserPublicKey;

    /**
     * 16 byte secret key, generated by the embedder, to be provided to the app server for use
     * in encrypting and authenticating messages sent to the {@link #endpoint}.
     *
     * @see <a href="https://www.w3.org/TR/push-api/#dom-pushencryptionkeyname-auth">PushEncryptionKeyName - auth</a>
     * @see <a href="https://tools.ietf.org/html/rfc8291#section-3.2">RFC 8291, section 3.2</a>
     */
    @NonNull
    public final byte[] authSecret;

    public WebPushSubscription(final @NonNull String scope,
                               final @NonNull String endpoint,
                               final @Nullable byte[] appServerKey,
                               final @NonNull byte[] browserPublicKey,
                               final @NonNull byte[] authSecret) {
        this.scope = scope;
        this.endpoint = endpoint;
        this.appServerKey = appServerKey;
        this.browserPublicKey = browserPublicKey;
        this.authSecret = authSecret;

        if (appServerKey != null) {
            if (appServerKey.length != P256_PUBLIC_KEY_LENGTH) {
                throw new IllegalArgumentException(String.format("appServerKey should be %d bytes", P256_PUBLIC_KEY_LENGTH));
            }

            if (Arrays.equals(appServerKey, browserPublicKey)) {
                throw new IllegalArgumentException("appServerKey and browserPublicKey must differ");
            }
        }

        if (browserPublicKey.length != P256_PUBLIC_KEY_LENGTH) {
            throw new IllegalArgumentException(String.format("browserPublicKey should be %d bytes", P256_PUBLIC_KEY_LENGTH));
        }

        if (authSecret.length != 16) {
            throw new IllegalArgumentException("authSecret must be 128 bits");
        }
    }

    private WebPushSubscription(final Parcel in) {
        this.scope = in.readString();
        this.endpoint = in.readString();

        if (ParcelableUtils.readBoolean(in)) {
            this.appServerKey = new byte[P256_PUBLIC_KEY_LENGTH];
            in.readByteArray(this.appServerKey);
        } else {
            appServerKey = null;
        }

        this.browserPublicKey = new byte[P256_PUBLIC_KEY_LENGTH];
        in.readByteArray(this.browserPublicKey);

        this.authSecret = new byte[16];
        in.readByteArray(this.authSecret);
    }

    /* package */ GeckoBundle toBundle() {
        final GeckoBundle bundle = new GeckoBundle(5);
        bundle.putString("scope", scope);
        bundle.putString("endpoint", endpoint);
        if (appServerKey != null) {
            bundle.putString("appServerKey", Base64Utils.encode(appServerKey));
        }
        bundle.putString("browserPublicKey", Base64Utils.encode(browserPublicKey));
        bundle.putString("authSecret", Base64Utils.encode(authSecret));
        return bundle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(scope);
        out.writeString(endpoint);

        ParcelableUtils.writeBoolean(out, appServerKey != null);
        if (appServerKey != null) {
            out.writeByteArray(appServerKey);
        }

        out.writeByteArray(browserPublicKey);
        out.writeByteArray(authSecret);
    }

    public static final Parcelable.Creator<WebPushSubscription> CREATOR = new Parcelable.Creator<WebPushSubscription>() {
        @Override
        @AnyThread
        public WebPushSubscription createFromParcel(final Parcel parcel) {
            return new WebPushSubscription(parcel);
        }

        @Override
        @AnyThread
        public WebPushSubscription[] newArray(final int size) {
            return new WebPushSubscription[size];
        }
    };
}