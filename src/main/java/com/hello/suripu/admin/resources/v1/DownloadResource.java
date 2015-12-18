package com.hello.suripu.admin.resources.v1;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hello.suripu.admin.models.FirmwareUpdate;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import com.hello.suripu.coredw8.oauth.Auth;
import com.hello.suripu.coredw8.oauth.ScopesAllowed;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/download")
public class DownloadResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadResource.class);

    final private AmazonS3Client amazonS3Client;
    final private String bucketName;

    public DownloadResource(final AmazonS3Client amazonS3Client, final String bucketName) {
        this.amazonS3Client = amazonS3Client;
        this.bucketName = bucketName;
    }


    private List<FirmwareUpdate> createFirmwareUpdatesFromListing(final Collection<S3ObjectSummary> summaries) {
        final Date expiration = DateTime.now().plusHours(1).toDate();
        return summaries.stream()
                .filter(summary -> {
                    final String key = summary.getKey();
                    return (key.endsWith(".hex") || key.endsWith(".bin") || key.endsWith(".zip"));
                })
                .map(summary -> {
                    final String key = summary.getKey();
                    final GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, key);
                    generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
                    generatePresignedUrlRequest.setExpiration(expiration);

                    final URL url = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);

                    LOGGER.debug("Generated url for key = {}", key);
                    return new FirmwareUpdate(key, url.toExternalForm(), summary.getLastModified().getTime());
                })
                .sorted(FirmwareUpdate.createOrdering())
                .collect(Collectors.toList());
    }


    @ScopesAllowed({OAuthScope.FIRMWARE_UPDATE})
    @Path("/pill/firmware/stable")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareUpdate> getStablePillFirmware(@Auth AccessToken accessToken) {
        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("pill_stable");

        final ObjectListing objectListing = amazonS3Client.listObjects(listObjectsRequest);
        return createFirmwareUpdatesFromListing(objectListing.getObjectSummaries());
    }

    @ScopesAllowed({OAuthScope.FIRMWARE_UPDATE})
    @Path("/pill/firmware")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareUpdate> getUnstablePillFirmware(@Auth AccessToken accessToken) {
        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.withBucketName(bucketName);
        listObjectsRequest.withPrefix("pill_unstable");

        final ObjectListing objectListing = amazonS3Client.listObjects(listObjectsRequest);
        return createFirmwareUpdatesFromListing(objectListing.getObjectSummaries());
    }
}
