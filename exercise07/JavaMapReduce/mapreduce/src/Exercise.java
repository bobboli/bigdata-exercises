import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

// Results on gutenberg.txt:
// Q1   42590
// Q2   5154566
// Q3   1511710

public class Exercise {
    
    private static class WordGroupingMapper extends Mapper<Object, Text, Text, IntWritable>{
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while(itr.hasMoreTokens())
            {
                boolean bEmit = false;
                String wordStr = itr.nextToken();
                if(wordStr.charAt(0) == 'H')    // Question 1: number of distinct words begin with H
                //if(wordStr.charAt(wordStr.length()-1)=='g')    // Question 2: number of words (not distinct) end with g
                //if(wordStr.length()==12)    // Question 3: number of words (not distinct) of length 12
                {
                    bEmit = true;
                }

                if(bEmit){
                    word.set(wordStr);
                    context.write(word, one);
                }
            }
        }
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
    }


    private static class WordGroupingReducer extends Reducer<Text,IntWritable,Text,IntWritable> {
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            if(false) // Question 1: number of distinct words begin with H
            //if(true)  // Question 2: number of words (not distinct) end with g
            //if(true)  // Question 3: number of words (not distinct) of length 12
            {
                int resultVal = 0;
                for(IntWritable v : values)
                {
                    resultVal += v.get(); 
                }
                context.write(key, new IntWritable(resultVal));
            }
            else
            {
                context.write(key, new IntWritable(1));
            }
        }
    }


    private static class CountMapper extends Mapper<Text, IntWritable, IntWritable, IntWritable>{
        private final static IntWritable commonKey = new IntWritable(1);
        public void map(Text key, IntWritable value, Context context) throws IOException, InterruptedException {
            context.write(commonKey, value); // Just want the word count, all keys should be same
        }
    }
    private static class CountReducer extends Reducer<IntWritable,IntWritable,IntWritable,IntWritable> {
        private IntWritable result = new IntWritable();
        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int resultVal = 0;
            for(IntWritable v : values)
            {
                resultVal += v.get(); 
            }
            result.set(resultVal);
            context.write(new IntWritable(1), result);  // The key is a dummy one
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: 2 required params <hdfs_file_in> <hdfs_file_out>");
            System.exit(2);
        }

        Path inputFilePath = new Path(otherArgs[0]);
        Path outputIntermediatePath = new Path(otherArgs[1] + "/intermediate");
        Path outputIntermediateFile0Path = new Path(otherArgs[1] + "/intermediate/part-r-00000");
        Path outputResultPath = new Path(otherArgs[1] + "/result");

        // First, group words
        boolean bFirstJobSuccess = false;
        {
            Job job = new Job(conf, "WordGrouping");
            job.setJarByClass(Exercise.class);
            job.setMapperClass(WordGroupingMapper.class);
            job.setReducerClass(WordGroupingReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(IntWritable.class);
            FileInputFormat.addInputPath(job, inputFilePath);
            SequenceFileOutputFormat.setOutputPath(job, outputIntermediatePath);
            job.setOutputFormatClass(SequenceFileOutputFormat.class);
            
            bFirstJobSuccess = job.waitForCompletion(true);
        }

        // Then, count the frequency of all words
        if(bFirstJobSuccess)
        {
            Job job = new Job(conf, "WordCounting");
            job.setJarByClass(Exercise.class);
            job.setMapperClass(CountMapper.class);
            job.setReducerClass(CountReducer.class);
            job.setOutputKeyClass(IntWritable.class);
            job.setOutputValueClass(IntWritable.class);
            SequenceFileInputFormat.addInputPath(job, outputIntermediateFile0Path);
            job.setInputFormatClass(SequenceFileInputFormat.class);
            FileOutputFormat.setOutputPath(job, outputResultPath);

            System.exit(job.waitForCompletion(true) ? 0 : 1);
        }
    }

}
