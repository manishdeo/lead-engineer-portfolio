import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.time.Duration;
import java.util.Properties;

/**
 * Real-time Analytics Pipeline using Apache Flink.
 * This example reads events from Kafka, counts them in 1-minute tumbling windows,
 * and writes the aggregated results back to another Kafka topic.
 */
public class RealTimeAnalyticsPipeline {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 1. Configure Kafka Consumer
        Properties consumerProps = new Properties();
        consumerProps.setProperty("bootstrap.servers", "localhost:9092");
        consumerProps.setProperty("group.id", "flink-analytics-group");
        
        FlinkKafkaConsumer<String> kafkaSource = new FlinkKafkaConsumer<>(
            "user-events-topic", 
            new SimpleStringSchema(), 
            consumerProps
        );

        // 2. Create DataStream from Kafka
        DataStream<String> eventStream = env.addSource(kafkaSource)
            .assignTimestampsAndWatermarks(
                WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                    .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis())
            );

        // 3. Process the stream: count events per minute
        DataStream<String> aggregatedStream = eventStream
            .keyBy(event -> "events") // Group all events into one key for a global count
            .window(TumblingEventTimeWindows.of(Time.minutes(1)))
            .apply((key, window, events, out) -> {
                long count = 0;
                for (String event : events) {
                    count++;
                }
                String result = String.format("Window: %s, Count: %d", window.getEnd(), count);
                out.collect(result);
            });

        // 4. Configure Kafka Producer
        Properties producerProps = new Properties();
        producerProps.setProperty("bootstrap.servers", "localhost:9092");
        
        FlinkKafkaProducer<String> kafkaSink = new FlinkKafkaProducer<>(
            "analytics-results-topic",
            new SimpleStringSchema(),
            producerProps
        );

        // 5. Write aggregated results to Kafka
        aggregatedStream.addSink(kafkaSink);

        // Execute the Flink job
        env.execute("Real-time Analytics Pipeline");
    }
}
