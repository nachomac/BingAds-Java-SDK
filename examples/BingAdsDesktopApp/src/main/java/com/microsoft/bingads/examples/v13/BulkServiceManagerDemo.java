package com.microsoft.bingads.examples.v13;

import com.microsoft.bingads.v13.bulk.entities.*;
import com.microsoft.bingads.v13.bulk.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public class BulkServiceManagerDemo extends BulkExampleBase {
    
    public static void main(String[] args) {
		
        BulkEntityIterable downloadEntities = null;

        try {
            authorizationData = getAuthorizationData();

            BulkServiceManager = new BulkServiceManager(
                    authorizationData, 
                    API_ENVIRONMENT);
            
            BulkServiceManager.setStatusPollIntervalInMilliseconds(5000);
            
            // Download all campaigns, ad groups, and ads in the account.

            ArrayOfDownloadEntity entities = new ArrayOfDownloadEntity();
            entities.getDownloadEntities().add(DownloadEntity.CAMPAIGNS);
            entities.getDownloadEntities().add(DownloadEntity.AD_GROUPS);
            entities.getDownloadEntities().add(DownloadEntity.ADS);

            // Optionally you can request quality score data for the requested bulk records.

            List<DataScope> dataScopes = new ArrayList<DataScope>();
            dataScopes.add(DataScope.ENTITY_DATA);
            dataScopes.add(DataScope.QUALITY_SCORE_DATA);

            // DownloadParameters is used for Option A below.

            DownloadParameters downloadParameters = new DownloadParameters();
            downloadParameters.setCampaignIds(null);
            downloadParameters.setDataScope(dataScopes);
            downloadParameters.setDownloadEntities(entities);
            downloadParameters.setFileType(DownloadFileType.CSV);
            downloadParameters.setLastSyncTimeInUTC(null); 
            downloadParameters.setResultFileDirectory(new File(FileDirectory));
            downloadParameters.setResultFileName(DownloadFileName);
            downloadParameters.setOverwriteResultFile(true);

            // SubmitDownloadParameters is used for Option B and Option C below.

            SubmitDownloadParameters submitDownloadParameters = new SubmitDownloadParameters();
            submitDownloadParameters.setCampaignIds(null);
            submitDownloadParameters.setDataScope(dataScopes);
            submitDownloadParameters.setDownloadEntities(entities);
            submitDownloadParameters.setFileType(DownloadFileType.CSV);
            submitDownloadParameters.setLastSyncTimeInUTC(null); 

            // Option A - Background Completion with BulkServiceManager
            // You can submit a download or upload request and the BulkServiceManager will automatically 
            // return results. The BulkServiceManager abstracts the details of checking for result file 
            // completion, and you don't have to write any code for results polling.

            outputStatusMessage("-----\nAwaiting Background Completion...");
            backgroundCompletionAsync(downloadParameters);

            // Alternatively we can use downloadEntitiesAsync if we want to work with the entities in memory.
            // If you enable this option the result file from BackgroundCompletionAsync will also be deleted
            // if written to the same working directory.
            outputStatusMessage("-----\nAwaiting Background Completion with DownloadEntitiesAsync...");
            List<BulkEntity> resultEntities = downloadEntitiesAsync(downloadParameters);

            // Option B - Submit and Download with BulkServiceManager
            // Submit the download request and then use the BulkDownloadOperation result to 
            // track status until the download is complete e.g. either using
            // trackAsync or getStatusAsync.

            outputStatusMessage("-----\nAwaiting Submit and Download...");
            submitTrackDownloadAsync(submitDownloadParameters);

            // Option C - Download Results with BulkServiceManager
            // If for any reason you have to resume from a previous application state, 
            // you can use an existing download request identifier and use it 
            // to download the result file. Use trackAsync to indicate that the application 
            // should wait to ensure that the download status is completed.

            // For example you might have previously retrieved a request ID using SubmitDownloadAsync.
            BulkDownloadOperation bulkDownloadOperation = BulkServiceManager.submitDownloadAsync(submitDownloadParameters, null).get();
            java.lang.String requestId = bulkDownloadOperation.getRequestId();

            // Given the request ID above, you can resume the workflow and download the bulk file.
            // The download request identifier is valid for two days. 
            // If you do not download the bulk file within two days, you must request it again.
            outputStatusMessage("-----\nAwaiting Download Results...");
            downloadResultsAsync(requestId);
        }
        catch (Exception ex) {
            String faultXml = ExampleExceptionHelper.getBingAdsExceptionFaultXml(ex, System.out);
            outputStatusMessage(faultXml);
            String message = ExampleExceptionHelper.handleBingAdsSDKException(ex, System.out);
            outputStatusMessage(message);
        } 
        finally {
            if (downloadEntities != null){
                try {
                    downloadEntities.close();
                } 
                catch (IOException ex) {
                    outputStatusMessage(ex.getMessage());
                }
            }
        }
    }
    
    // Writes the specified entities to a local temporary file prior to upload.
    static List<BulkEntity> uploadEntitiesAsync(List<BulkEntity> uploadEntities) throws IOException, ExecutionException, InterruptedException 
    {
    	// The system temp directory will be used if another working directory is not specified. If you are 
        // using a cloud service such as Microsoft Azure you'll want to ensure you do not exceed the file or directory limits. 
        // You can specify a different working directory for each BulkServiceManager instance.

        BulkServiceManager.setWorkingDirectory(new File(FileDirectory));

        EntityUploadParameters entityUploadParameters = new EntityUploadParameters();
        entityUploadParameters.setEntities(uploadEntities);
        entityUploadParameters.setOverwriteResultFile(true);
        entityUploadParameters.setResultFileDirectory(new File(FileDirectory));
        entityUploadParameters.setResultFileName(ResultFileName);
        entityUploadParameters.setResponseMode(ResponseMode.ERRORS_AND_RESULTS);
        
        // The uploadEntitiesAsync method returns BulkEntityIterable, so the result file will not
        // be accessible e.g. for cleanupTempFiles until you iterate over the result and close the BulkEntityIterable instance.
        
        BulkEntityIterable tempEntities = BulkServiceManager.uploadEntitiesAsync(entityUploadParameters, null, null).get();
        
        ArrayList<BulkEntity> resultEntities = new ArrayList<BulkEntity>();
        for (BulkEntity entity : tempEntities) {
            resultEntities.add(entity);
        }
        
        tempEntities.close();
        
        // The cleanupTempFiles method removes all files (not sub-directories) within the working directory, 
        // whether or not the files were created by this BulkServiceManager instance. 
        
        //BulkServiceManager.cleanupTempFiles();
        
        return resultEntities;
    }
    
    // Writes the specified entities to a local temporary file after download. 
    static List<BulkEntity> downloadEntitiesAsync(DownloadParameters downloadParameters) throws IOException, ExecutionException, InterruptedException 
    {
    	// The system temp directory will be used if another working directory is not specified. If you are 
        // using a cloud service such as Microsoft Azure you'll want to ensure you do not exceed the file or directory limits. 
        // You can specify a different working directory for each BulkServiceManager instance.

        BulkServiceManager.setWorkingDirectory(new File(FileDirectory));
        
        // The downloadEntitiesAsync method returns BulkEntityIterable, so the download file will not
        // be accessible e.g. for cleanupTempFiles until you iterate over the result and close the BulkEntityIterable instance.
        
        BulkEntityIterable tempEntities = BulkServiceManager.downloadEntitiesAsync(downloadParameters, null, null).get();
        
        ArrayList<BulkEntity> resultEntities = new ArrayList<BulkEntity>();
        for (BulkEntity entity : tempEntities) {
            resultEntities.add(entity);
        }
        
        tempEntities.close();
        
        // The cleanupTempFiles method removes all files (not sub-directories) within the working directory, 
        // whether or not the files were created by this BulkServiceManager instance. 
        
        //BulkServiceManager.cleanupTempFiles();
        
        return resultEntities;
    }
    
    // You can submit a download or upload request and the BulkServiceManager will automatically 
    // return results. The BulkServiceManager abstracts the details of checking for result file 
    // completion, and you don't have to write any code for results polling.
    private static void backgroundCompletionAsync(DownloadParameters downloadParameters) 
    	throws ExecutionException, InterruptedException, TimeoutException 
    {
        
    	// You may optionally cancel the downloadFileAsync operation after a specified time interval. 
        File resultFile = BulkServiceManager.downloadFileAsync(downloadParameters, null).get(TimeoutInMilliseconds, TimeUnit.MILLISECONDS);

        outputStatusMessage(String.format("Download result file: %s", resultFile.getName()));
    }
    
    // Submit the download request and then use the BulkDownloadOperation result to 
    // track status until the download is complete using trackAsync.
    private static void submitTrackDownloadAsync(SubmitDownloadParameters submitDownloadParameters) 
        throws ExecutionException, InterruptedException, URISyntaxException, IOException, TimeoutException 
    {
        BulkDownloadOperation bulkDownloadOperation = BulkServiceManager.submitDownloadAsync(submitDownloadParameters, null).get();

        // You may optionally cancel the trackAsync operation after a specified time interval.
        BulkOperationStatus<DownloadStatus> downloadStatus = 
            bulkDownloadOperation.trackAsync(null).get(TimeoutInMilliseconds, TimeUnit.MILLISECONDS);
        
        File resultFile = bulkDownloadOperation.downloadResultFileAsync(
            new File(FileDirectory),
            ResultFileName,
            true, // Set this value to true if you want to decompress the ZIP file.
            true,  // Set this value true if you want to overwrite the named file.
            null).get();

        outputStatusMessage(String.format("Download result file: %s", resultFile.getName()));
    }
    
    // Submit the download request and then use the BulkDownloadOperation result to 
    // track status until the download is complete using getStatusAsync.
    private static void submitPollDownloadAsync(SubmitDownloadParameters submitDownloadParameters) 
        throws ExecutionException, InterruptedException, URISyntaxException, IOException, TimeoutException 
    {
        BulkDownloadOperation bulkDownloadOperation = BulkServiceManager.submitDownloadAsync(submitDownloadParameters, null).get();

	BulkOperationStatus<DownloadStatus> downloadStatus;
		
	for (int i = 0; i < 24; i++)
	{
	    Thread.sleep(5000);
	    downloadStatus = bulkDownloadOperation.getStatusAsync(null).get(TimeoutInMilliseconds, TimeUnit.MILLISECONDS);
	    if (downloadStatus.getStatus() == DownloadStatus.COMPLETED)
	    {
	        break;
	    }
	}

        File resultFile = bulkDownloadOperation.downloadResultFileAsync(
            new File(FileDirectory),
            ResultFileName,
            true, // Set this value to true if you want to decompress the ZIP file.
            true,  // Set this value true if you want to overwrite the named file.
            null).get();

        outputStatusMessage(String.format("Download result file: %s", resultFile.getName()));
    }
    
    // If for any reason you have to resume from a previous application state, 
    // you can use an existing download request identifier and use it 
    // to download the result file. Use trackAsync to indicate that the application 
    // should wait to ensure that the download status is completed.
    private static void downloadResultsAsync(java.lang.String requestId) 
        throws ExecutionException, InterruptedException, URISyntaxException, IOException, TimeoutException 
    {

        BulkDownloadOperation bulkDownloadOperation = new BulkDownloadOperation(requestId, authorizationData, API_ENVIRONMENT);

        bulkDownloadOperation.setStatusPollIntervalInMilliseconds(5000);

        // You can use trackAsync to poll until complete as shown here, 
        // or use custom polling logic with getStatusAsync.
		
        // You may optionally cancel the trackAsync operation after a specified time interval.
        BulkOperationStatus<DownloadStatus> downloadStatus = 
                        bulkDownloadOperation.trackAsync(null).get(TimeoutInMilliseconds, TimeUnit.MILLISECONDS);

        File resultFile = bulkDownloadOperation.downloadResultFileAsync(
            new File(FileDirectory),
            ResultFileName,
            true, // Set this value to true if you want to decompress the ZIP file
            true,  // Set this value true if you want to overwrite the named file.
            null).get();

        outputStatusMessage(String.format("Download result file: %s", resultFile.getName()));
        outputStatusMessage(String.format("Status: %s", downloadStatus.getStatus()));
        outputStatusMessage(String.format("TrackingId: %s", downloadStatus.getTrackingId()));
    }
}
