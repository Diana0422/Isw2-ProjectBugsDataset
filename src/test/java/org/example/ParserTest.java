package org.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.example.logic.model.utils.Parser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit test for simple App.
 */
@RunWith(Parameterized.class)
public class ParserTest
{
    /**
     * Rigorous Test :-)
     */

    private final String line;
    private final String expectedResult;
    private final boolean old;

    public ParserTest(String line, boolean old, String expectedResult) {
        this.line = line;
        this.old = old;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"{api => stream/api}/src/com/File.java", true, "api/src/com/File.java"},
                {"{api => stream/api}/src/com/File.java", false, "stream/api/src/com/File.java"},
                {"pippo/franco/{api => stream/api}/src/com/File.java", true, "pippo/franco/api/src/com/File.java"},
                {"pippo/franco/{api => stream/api}/src/com/File.java", false, "pippo/franco/stream/api/src/com/File.java"},
                {"pippo/franco/stream/src/com/{Old.java => New.java}", false, "pippo/franco/stream/src/com/New.java"},
                {"pippo/franco/stream/src/com/{Old.java => New.java}", true, "pippo/franco/stream/src/com/Old.java"},
                {"pippo/franco/stream/src/com/File.java", false, "pippo/franco/stream/src/com/File.java"},
                {"bookkeeper-server/src/test/java/org/apache/bookkeeper/bookie/{TestEntryLog.java => EntryLogTest.java}", false, "bookkeeper-server/src/test/java/org/apache/bookkeeper/bookie/EntryLogTest.java"},
                {"bookkeeper-server/src/{test/java/org/apache/bookkeeper/meta/HierarchicalLedgerDeleteTest.java => main/java/org/apache/bookkeeper/versioning/Version.java}", false, "bookkeeper-server/src/main/java/org/apache/bookkeeper/versioning/Version.java"},
                {"hedwig-client/src/main/java/org/apache/hedwig/client/netty/{ => impl}/ClientChannelPipelineFactory.java", true, "hedwig-client/src/main/java/org/apache/hedwig/client/netty/ClientChannelPipelineFactory.java"},
                {"hedwig-client/src/main/java/org/apache/hedwig/client/netty/impl/{multiplex => }/ResubscribeCallback.java", true, "hedwig-client/src/main/java/org/apache/hedwig/client/netty/impl/multiplex/ResubscribeCallback.java"},
                {"hedwig-client/src/main/java/org/apache/hedwig/client/netty/impl/{multiplex => }/ResubscribeCallback.java", false, "hedwig-client/src/main/java/org/apache/hedwig/client/netty/impl/ResubscribeCallback.java"},
                {"streamstore/client/src/main/java/org/apache/distributedlog/stream/client/resolver/AbstractStreamResolverFactory.java => bookkeeper-common/src/main/java/org/apache/bookkeeper/common/resolver/AbstractNameResolverFactory.java", false, "bookkeeper-common/src/main/java/org/apache/bookkeeper/common/resolver/AbstractNameResolverFactory.java"},
                {"streamstore/client/src/main/java/org/apache/distributedlog/stream/client/resolver/AbstractStreamResolverFactory.java => bookkeeper-common/src/main/java/org/apache/bookkeeper/common/resolver/AbstractNameResolverFactory.java", true, "streamstore/client/src/main/java/org/apache/distributedlog/stream/client/resolver/AbstractStreamResolverFactory.java"},
        });
    }

    @Test
    public void testParseFilepathFromLine()
    {
        String s = Parser.getInstance().parseFilePathFromLine(this.line, this.old);
        assertEquals(expectedResult, s);
    }
}
