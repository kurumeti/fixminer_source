package edu.lu.uni.serval.fixminer.ediff;


import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class TestPredefinedCases {




    @Test
    public void testIFCase1() throws IOException {
        Properties appProps = new Properties();
        appProps.load(new FileInputStream("src/main/resource/app.properties"));
        String srcMLPath = appProps.getProperty("srcMLPath","FORKJOIN");

        File revFile = new File("src/main/resource/testFiles/if_example_1.c");
        File prevFile =new File("src/main/resource/testFiles/prev_if_example_1.c");

        EDiffHunkParser parser = new EDiffHunkParser();



        List<HierarchicalActionSet> hierarchicalActionSets = parser.parseChangedSourceCodeWithGumTree2(prevFile, revFile, srcMLPath,false);
        hierarchicalActionSets.size();
        Assert.assertEquals(hierarchicalActionSets.size(),1);
        Assert.assertEquals(hierarchicalActionSets.get(0).toString(),"UPD if@@if x >= 5 y += 4 @TO@ if x > 5 y += 4 @AT@ 2 @LENGTH@ 16\n" +
                "---UPD condition@@x >= 5 @TO@ x > 5 @AT@ 2 @LENGTH@ 9\n" +
                "------UPD expr@@x >= 5 @TO@ x > 5 @AT@ 3 @LENGTH@ 6\n" +
                "---------UPD operator@@>= @TO@ > @AT@ 5 @LENGTH@ 2\n");

    }
    @Test
    public void testForCase1() throws IOException {
        Properties appProps = new Properties();
        appProps.load(new FileInputStream("src/main/resource/app.properties"));
        String srcMLPath = appProps.getProperty("srcMLPath","FORKJOIN");

        File revFile = new File("src/main/resource/testFiles/for_example_1.c");
        File prevFile =new File("src/main/resource/testFiles/prev_for_example_1.c");



        EDiffHunkParser parser = new EDiffHunkParser();



        List<HierarchicalActionSet> hierarchicalActionSets = parser.parseChangedSourceCodeWithGumTree2(prevFile, revFile, srcMLPath,false);
        hierarchicalActionSets.size();
        Assert.assertEquals(hierarchicalActionSets.size(),1);
        Assert.assertEquals(hierarchicalActionSets.get(0).toString(),"UPD for@@i = 0 i < max i ++ if line ] == ' ' if line ] == '\\t' tab ++ @TO@ i = 0 i < max i ++ if line ] == ' ' space ++ if line ] == '\\t' tab ++ @AT@ 4 @LENGTH@ 60\n" +
                "---UPD block@@if line ] == ' ' if line ] == '\\t' tab ++ @TO@ if line ] == ' ' space ++ if line ] == '\\t' tab ++ @AT@ 26 @LENGTH@ 98\n" +
                "------UPD if@@if line ] == ' ' @TO@ if line ] == ' ' space ++ @AT@ 33 @LENGTH@ 16\n" +
                "---------UPD then@@ @TO@ space ++ @AT@ 54 @LENGTH@ 0\n" +
                "------------UPD block@@ @TO@ space ++ @AT@ 54 @LENGTH@ 22\n" +
                "---------------INS expr_stmt@@space ++ @TO@ block@@ @AT@ 62 @LENGTH@ 8\n" +
                "------------------INS expr@@space ++ @TO@ expr_stmt@@space ++ @AT@ 62 @LENGTH@ 8\n" +
                "---------------------INS name@@space @TO@ expr@@space ++ @AT@ 62 @LENGTH@ 5\n" +
                "---------------------INS operator@@++ @TO@ expr@@space ++ @AT@ 67 @LENGTH@ 2\n" +
                "---------------DEL continue@@ @AT@ 62 @LENGTH@ 0\n");

    }
    @Test
    public void testWhileCase1() throws IOException {
        Properties appProps = new Properties();
        appProps.load(new FileInputStream("src/main/resource/app.properties"));
        String srcMLPath = appProps.getProperty("srcMLPath", "FORKJOIN");

        File revFile = new File("src/main/resource/testFiles/while_example_1.c");
        File prevFile = new File("src/main/resource/testFiles/prev_while_example_1.c");


        EDiffHunkParser parser = new EDiffHunkParser();

        List<HierarchicalActionSet> hierarchicalActionSets = parser.parseChangedSourceCodeWithGumTree2(prevFile, revFile, srcMLPath,false);
        hierarchicalActionSets.size();
        Assert.assertEquals(hierarchicalActionSets.size(), 1);

        Assert.assertEquals(hierarchicalActionSets.get(0).toString(), "UPD do@@y = f x x -- print x x > 0 @TO@ y = f x x -- x > 0 @AT@ 0 @LENGTH@ 62\n" +
                "---UPD block@@y = f x x -- print x @TO@ y = f x x -- @AT@ 3 @LENGTH@ 42\n" +
                "------DEL expr_stmt@@print x @AT@ 33 @LENGTH@ 7\n" +
                "---------DEL expr@@print x @AT@ 33 @LENGTH@ 7\n" +
                "------------DEL call@@print x @AT@ 33 @LENGTH@ 7\n" +
                "---------------DEL name@@print @AT@ 33 @LENGTH@ 5\n" +
                "---------------DEL argument_list@@x @AT@ 38 @LENGTH@ 4\n" +
                "------------------DEL argument@@x @AT@ 39 @LENGTH@ 1\n" +
                "---------------------DEL expr@@x @AT@ 39 @LENGTH@ 1\n" +
                "------------------------DEL name@@x @AT@ 39 @LENGTH@ 1\n");
    }


        @Test
        public void testIFRetrun() throws IOException {
            Properties appProps = new Properties();
            appProps.load(new FileInputStream("src/main/resource/app.properties"));
            String srcMLPath = appProps.getProperty("srcMLPath","FORKJOIN");

            File revFile = new File("src/main/resource/testFiles/if_return.c");
            File prevFile =new File("src/main/resource/testFiles/prev_if_return.c");



            EDiffHunkParser parser = new EDiffHunkParser();

            List<HierarchicalActionSet> hierarchicalActionSets = parser.parseChangedSourceCodeWithGumTree2(prevFile, revFile, srcMLPath,false);
            hierarchicalActionSets.size();
//            Assert.assertEquals(hierarchicalActionSets.size(),1);
        }
        @Test
        public void testIfElse() throws IOException {
            Properties appProps = new Properties();
            appProps.load(new FileInputStream("src/main/resource/app.properties"));
            String srcMLPath = appProps.getProperty("srcMLPath","FORKJOIN");

            File revFile = new File("src/main/resource/testFiles/if_else.c");
            File prevFile =new File("src/main/resource/testFiles/prev_if_else.c");



            EDiffHunkParser parser = new EDiffHunkParser();

            List<HierarchicalActionSet> hierarchicalActionSets = parser.parseChangedSourceCodeWithGumTree2(prevFile, revFile, srcMLPath,false);
            hierarchicalActionSets.size();
    //            Assert.assertEquals(hierarchicalActionSets.size(),1);
        }

    @Test
    public void testStruct() throws IOException {
        Properties appProps = new Properties();
        appProps.load(new FileInputStream("src/main/resource/app.properties"));
        String srcMLPath = appProps.getProperty("srcMLPath","FORKJOIN");

        File revFile = new File("src/main/resource/testFiles/struct.c");
        File prevFile =new File("src/main/resource/testFiles/prev_struct.c");



        EDiffHunkParser parser = new EDiffHunkParser();

        List<HierarchicalActionSet> hierarchicalActionSets = parser.parseChangedSourceCodeWithGumTree2(prevFile, revFile, srcMLPath,false);
        hierarchicalActionSets.size();
        //            Assert.assertEquals(hierarchicalActionSets.size(),1);
    }






}