package org.example;

import org.example.logic.model.keyabstractions.JFile;
import org.example.logic.model.keyabstractions.Project;
import org.example.logic.model.keyabstractions.Release;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for simple App.
 */
@RunWith(Parameterized.class)
public class ReplicateReleaseTest {

    private final JFile file;
    private final List<Release> releases;
    private final List<Release> expectedResult;

    public ReplicateReleaseTest(int[] releaseIndexes, List<Integer> resultIndexes) {
        this.file = new JFile(new Project("", new Properties(2)), "", "",
                LocalDateTime.now());
        this.releases = new ArrayList<>();
        this.expectedResult = new ArrayList<>();
        for (int i = 0; i < releaseIndexes.length; i++) {
            Release rel = new Release(String.valueOf(i), String.valueOf(i), LocalDateTime.now());
            rel.setIndex(releaseIndexes[i]);
            releases.add(rel);
            file.addRelease(rel);
        }

        for (int i = 0; i < resultIndexes.size(); i++) {
            Release rel = new Release(String.valueOf(i), String.valueOf(i), LocalDateTime.now());
            rel.setIndex(resultIndexes.get(i));
            expectedResult.add(rel);
        }
    }


    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {new int[]{1, 2, 3, 7, 8}, Arrays.asList(1,2,3,4,5,6,7,8)}
        });
    }

    @Test
    public void testReplication() {
        file.fill(this.releases);
        for (int i = 0; i < expectedResult.size(); i++) {
            assertEquals(expectedResult.get(i).getIndex(), file.getReleases().get(i).getIndex());
        }
    }
}
