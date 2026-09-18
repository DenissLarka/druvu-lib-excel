/**
 * Excel reports from plain Java. The xlsx engine (fastexcel) is an implementation detail: it is required, never
 * re-exported, and no public type of this module mentions it.
 */
module com.druvu.excel {
    requires org.dhatim.fastexcel;

    exports com.druvu.excel;
}
