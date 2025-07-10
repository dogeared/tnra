# File Upload Setup

## Overview

The Profile view now supports file uploads for profile images. Images are stored on the file system instead of as base64 in the database for better performance and database efficiency.

## Configuration

### Application Properties

Add the following configuration to `application.yml`:

```yaml
app:
  file-storage:
    upload-dir: uploads          # Directory where files are stored
    base-url: /uploads          # URL path to serve files
```

### File Storage

- **Location**: Files are stored in the `uploads/` directory (relative to application root)
- **Naming**: Files are renamed with UUID to prevent conflicts
- **Access**: Files are served via `/uploads/{filename}` endpoint
- **Security**: Directory traversal attacks are prevented

## Components

### FileStorageService
- **Interface**: `FileStorageService`
- **Implementation**: `FileStorageServiceImpl`
- **Methods**:
  - `storeFile()` - Store uploaded file and return filename
  - `deleteFile()` - Delete file from storage
  - `getFileUrl()` - Get URL to access file

### FileController
- **Endpoint**: `/uploads/{fileName}`
- **Purpose**: Serve uploaded files
- **Security**: Prevents directory traversal
- **Content-Type**: Automatically determined from file extension

### ProfileView Updates
- **Upload Component**: Uses Vaadin Upload with MemoryBuffer
- **File Validation**: Only accepts image files (image/*)
- **Size Limit**: 5MB maximum file size
- **Cleanup**: Automatically deletes old profile image when new one is uploaded

## Usage

1. Navigate to Profile page (requires authentication)
2. Click "Upload Image" or drag & drop an image
3. Image is stored on file system with unique filename
4. Database stores only the filename reference
5. Image is displayed immediately after upload

## File Management

### Automatic Cleanup
- Old profile images are automatically deleted when new ones are uploaded
- Files are stored with UUID names to prevent conflicts

### Manual Cleanup
- Orphaned files can be cleaned up manually from the `uploads/` directory
- Consider implementing a scheduled task for cleanup if needed

## Security Considerations

- File uploads are restricted to authenticated users only
- Only image files are accepted
- File size is limited to 5MB
- Directory traversal attacks are prevented
- Files are served with appropriate content-type headers

## Performance Benefits

- **Database**: No large base64 strings stored in database
- **Memory**: Images loaded on-demand from file system
- **Network**: Images served directly by web server
- **Storage**: More efficient file system storage 