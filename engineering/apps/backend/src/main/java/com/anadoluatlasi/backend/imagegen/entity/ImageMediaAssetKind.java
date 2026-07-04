package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-007: Image media asset türü.
 *
 * <p>FP-007 kapsamında yalnızca AI tarafından üretilmiş
 * image varlıkları yer alır.</p>
 *
 * <p>Video/audio türleri FP-007 kapsamında yoktur.</p>
 */
public enum ImageMediaAssetKind {

    /** AI provider tarafından üretilmiş görsel */
    GENERATED_IMAGE
}
