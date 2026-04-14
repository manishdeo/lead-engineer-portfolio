import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

/**
 * Example of generating a pre-signed URL for an Amazon S3 object.
 * This is a secure pattern for allowing temporary, direct access to S3 files
 * without passing the traffic through your application servers.
 */
public class S3PresignedUrlGenerator {

    public String generatePresignedUrl(String bucketName, String objectKey, int expirationMinutes) {
        
        // S3Presigner requires the AWS SDK v2
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                // In a real app, use DefaultCredentialsProvider
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build()) {

            // Create the GetObjectRequest
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            // Create the PresignRequest, specifying the expiration time
            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            // Generate the presigned URL
            PresignedGetObjectRequest presignedGetObjectRequest =
                    presigner.presignGetObject(getObjectPresignRequest);

            return presignedGetObjectRequest.url().toString();

        } catch (Exception e) {
            System.err.println("Error generating presigned URL: " + e.getMessage());
            return null;
        }
    }
}
