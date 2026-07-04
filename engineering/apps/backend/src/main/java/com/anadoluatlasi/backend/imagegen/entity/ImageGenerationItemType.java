package com.anadoluatlasi.backend.imagegen.entity;

/**
 * FP-007: Image generation item türü.
 *
 * <p>Bir generation request'in sahne görseli mi yoksa
 * thumbnail görseli mi olduğunu belirler.</p>
 *
 * <p>Video/audio türleri FP-007 kapsamında yoktur.</p>
 */
public enum ImageGenerationItemType {

    /** Storyboard sahne görseli */
    SCENE_IMAGE,

    /** Thumbnail kapak görseli */
    THUMBNAIL_IMAGE
}
