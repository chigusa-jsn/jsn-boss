package com.chigusa.jsnboss.mob;

/** テーマ亜種の目印。効果とドロップはテーマで決まる（VariantEffects） */
public interface Themed {
    Theme theme();

    /** 鉱石テーマの段階（0=石 1=鉄 2=金 3=ダイヤ）。ほかのテーマは 0 */
    default int oreTier() {
        return 0;
    }
}
