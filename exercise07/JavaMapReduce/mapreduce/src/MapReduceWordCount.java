import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class MapReduceWordCount {

    private static class WordCountMapper extends Mapper<Object, Text, Text, IntWritable>{

        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        // Only one input for map in this example. value is the text. key? idk
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while(itr.hasMoreTokens())
            {
                word.set(itr.nextToken());
                context.write(word, one);
            }
        }
    }

    private static class WordCountReducer extends Reducer<Text,IntWritable,Text,IntWritable> {

        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int resultVal = 0;
            for(IntWritable v : values)
            {
                resultVal += v.get(); 
            }
            result.set(resultVal);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: 2 required params <hdfs_file_in> <hdfs_file_out>");
            System.exit(2);
        }
        // Setup a MapReduce job
        Job job = new Job(conf, "word count");
        job.setJarByClass(MapReduceWordCount.class);
        job.setMapperClass(WordCountMapper.class);
        
	// Can you reducer be used as a combiner? If so, enable it.
	// You can also experiment with and without combiner and compare the running time.
	// job.setCombinerClass(WordCountReducer.class);
        job.setReducerClass(WordCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
	// at least one reducer, experiment with this parameter
        //job.setNumReduceTasks(8);    
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    // Performance on 0.1x dataset:
    // raw:                                 58s
    // with combiner:                       44s
    // with 8 reduce tasks:                 1m16s
    // With combiner & 8 reduce tasks:      1m3s
    }

}
