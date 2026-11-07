/**
 * Excel files into plain Java. The xlsx engine (fastexcel-reader) is an implementation detail: it is required, never
 * re-exported, and no public type of this module mentions it.
 */
module com.druvu.excel.reader {
    requires org.dhatim.fastexcel.reader;
}
