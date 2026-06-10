# OSS表单直传使用指南

## 🎯 核心优势

完全不需要后端做中转！前端直接上传到OSS，后端只提供签名。

```
传统方案：文件 → 后端 → OSS （占用服务器带宽）
直传方案：文件 → OSS （不占用服务器带宽）
```

---

## 🏗️ 架构流程

### 完整的上传流程

```
┌─────────────────────────────────────────────────────────────┐
│                        前端                                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 1. 请求上传签名
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       后端服务器                                │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  UploadController                                      │  │
│  │  POST /upload/signature                                 │  │
│  └───────────────────────────────────────────────────────┘  │
│                              │                               │
│                              ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  OssDirectUploadService                               │  │
│  │  - 生成上传策略（限制路径、大小、类型）                    │  │
│  │  - 使用SecretKey签名                                    │  │
│  │  - 返回签名信息                                         │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 2. 返回签名信息
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        前端                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  阿里云OSS SDK                                          │  │
│  │  - 使用签名信息初始化                                    │  │
│  │  - 直接上传到OSS                                        │  │
│  │  - 显示上传进度                                         │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 3. 直接上传到OSS
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      阿里云OSS                                 │
│  Bucket: auction-products                                    │
│  Path: products/USER_ID/2026/06/07/uuid.jpg                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 4. 返回图片URL
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        前端                                  │
│  接收URL: https://cdn.yourdomain.com/products/.../uuid.jpg   │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 5. 提交商品信息（包含图片URL）
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       后端服务器                               │
│  保存商品信息到数据库（imageUrl字段存储OSS URL）               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 前端完整示例

### 1. React + 阿里云OSS SDK

```javascript
import React, { useState } from 'react';
import OSS from 'ali-oss';

/**
 * OSS图片上传组件（签名方式）
 */
function OssImageUpload() {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [imageUrl, setImageUrl] = useState('');

  /**
   * 上传图片到OSS
   */
  const uploadToOSS = async (file) => {
    try {
      setUploading(true);
      setProgress(0);

      // 1. 获取上传签名
      const signature = await getUploadSignature(file.name);
      console.log('获取签名成功:', signature);

      // 2. 初始化OSS客户端（使用签名）
      const client = new OSS({
        region: signature.region,
        accessKeyId: signature.accessKeyId,
        accessKeySecret: '',  // 签名方式不需要SecretKey
        bucket: signature.bucket,
        secure: true,
        // 使用签名方式上传
        stsToken: null,
        refreshSTSToken: null,
        // 自定义上传方式
        uploadUrl: signature.endpoint
      });

      // 3. 生成唯一文件名
      const fileName = `${Date.now()}_${Math.random().toString(36).substring(7)}_${file.name}`;
      const objectKey = signature.uploadDir + fileName;

      // 4. 使用表单直传方式上传
      const result = await client.post(objectKey, file, {
        headers: {
          'Content-Type': file.type
        },
        // 使用签名和策略
        meta: {
          policy: signature.policy,
          signature: signature.signature,
          'AccessKeyId': signature.accessKeyId
        }
      });

      // 5. 获取图片URL
      const imageUrl = `https://${signature.bucket}.${signature.region}.aliyuncs.com/${objectKey}`;

      console.log('上传成功:', imageUrl);
      setImageUrl(imageUrl);
      setProgress(100);

      return imageUrl;

    } catch (error) {
      console.error('上传失败:', error);
      throw error;
    } finally {
      setUploading(false);
    }
  };

  /**
   * 获取上传签名
   */
  const getUploadSignature = async (fileName) => {
    const response = await fetch('/upload/signature?fileName=' + fileName, {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + getToken(),
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error('获取签名失败');
    }

    const data = await response.json();
    return data.data;
  };

  /**
   * 文件选择处理
   */
  const handleFileChange = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // 验证文件
    if (!file.type.startsWith('image/')) {
      alert('只能上传图片文件');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      alert('文件大小不能超过10MB');
      return;
    }

    try {
      await uploadToOSS(file);
      alert('上传成功！');
    } catch (error) {
      alert('上传失败: ' + error.message);
    }
  };

  return (
    <div className="oss-upload">
      <input
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        disabled={uploading}
      />

      {uploading && (
        <div>
          <p>上传中... {progress}%</p>
          <progress value={progress} max={100} />
        </div>
      )}

      {imageUrl && (
        <div>
          <img src={imageUrl} alt="预览" style={{ maxWidth: 200 }} />
          <p>URL: {imageUrl}</p>
        </div>
      )}

      <button onClick={() => createProduct(imageUrl)}>
        创建商品
      </button>
    </div>
  );
}
```

### 2. 使用FormData直接上传

```javascript
/**
 * 使用FormData上传（更简单的方式）
 */
async function uploadImageWithFormData(file) {
  try {
    // 1. 获取上传签名
    const signature = await getUploadSignature(file.name);

    // 2. 创建FormData
    const formData = new FormData();
    formData.append('file', file);
    formData.append('OSSAccessKeyId', signature.accessKeyId);
    formData.append('policy', signature.policy);
    formData.append('signature', signature.signature);
    formData.append('key', signature.uploadDir + file.name);
    formData.append('Content-Type', file.type);

    // 3. 直接POST到OSS
    const uploadUrl = `https://${signature.bucket}.${signature.region}.aliyuncs.com`;
    const response = await fetch(uploadUrl, {
      method: 'POST',
      body: formData
    });

    if (response.ok) {
      const imageUrl = `${uploadUrl}/${signature.uploadDir}${file.name}`;
      console.log('上传成功:', imageUrl);
      return imageUrl;
    } else {
      throw new Error('上传失败');
    }

  } catch (error) {
    console.error('上传失败:', error);
    throw error;
  }
}
```

### 3. Vue + Element UI示例

```vue

<template>
  <div class="oss-upload">
    <el-upload
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
        accept="image/*"
        drag
    >
      <el-icon class="el-icon--upload">
        <upload-filled/>
      </el-icon>
      <div class="el-upload__text">
        拖拽文件到此处或<em>点击上传</em>
      </div>
    </el-upload>

    <el-progress v-if="uploading" :percentage="uploadProgress"/>

    <img v-if="imageUrl" :src="imageUrl" class="preview-image"/>
  </div>
</template>

<script>
  import {ref} from './vue';
  import axios from 'axios';

  export default {
    setup() {
      const uploading = ref(false);
      const uploadProgress = ref(0);
      const imageUrl = ref('');

      const handleFileChange = async (file) => {
        if (!file.raw.type.startsWith('image/')) {
          ElMessage.error('只能上传图片文件');
          return;
        }

        uploading.value = true;
        uploadProgress.value = 0;

        try {
          const url = await uploadToOSS(file.raw);
          imageUrl.value = url;
          ElMessage.success('上传成功');
        } catch (error) {
          ElMessage.error('上传失败: ' + error.message);
        } finally {
          uploading.value = false;
          uploadProgress.value = 0;
        }
      };

      const uploadToOSS = async (file) => {
        // 1. 获取签名
        const {data: signature} = await axios.post('/upload/signature', null, {
          params: {fileName: file.name}
        });

        // 2. 直接上传到OSS
        const formData = new FormData();
        formData.append('file', file);
        formData.append('OSSAccessKeyId', signature.accessKeyId);
        formData.append('policy', signature.policy);
        formData.append('signature', signature.signature);
        formData.append('key', signature.uploadDir + file.name);
        formData.append('Content-Type', file.type);

        const uploadUrl = `https://${signature.bucket}.${signature.region}.aliyuncs.com`;
        const response = await axios.post(uploadUrl, formData, {
          onUploadProgress: (progressEvent) => {
            uploadProgress.value = Math.round(
                (progressEvent.loaded * 100) / progressEvent.total
            );
          }
        });

        return `${uploadUrl}/${signature.uploadDir}${file.name}`;
      };

      return {
        uploading,
        uploadProgress,
        imageUrl,
        handleFileChange
      };
    }
  };
</script>
```

### 4. React Native示例

```javascript
import * as ImagePicker from 'expo-image-picker';

/**
 * 移动端上传图片
 */
async function pickAndUploadImage() {
  // 1. 选择图片
  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ImagePicker.MediaTypeOptions.Images,
    allowsEditing: true,
    quality: 1,
  });

  if (!result.canceled) {
    const asset = result.assets[0];

    try {
      // 2. 获取签名
      const signature = await getUploadSignature(asset.fileName);

      // 3. 上传到OSS
      const formData = new FormData();
      formData.append('file', {
        uri: asset.uri,
        type: 'image/jpeg',
        name: asset.fileName,
      } as any);
      formData.append('OSSAccessKeyId', signature.accessKeyId);
      formData.append('policy', signature.policy);
      formData.append('signature', signature.signature);
      formData.append('key', signature.uploadDir + asset.fileName);

      const uploadUrl = `https://${signature.bucket}.${signature.region}.aliyuncs.com`;
      const response = await fetch(uploadUrl, {
        method: 'POST',
        body: formData,
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      const data = await response.json();
      console.log('上传成功:', data.url);
      return data.url;

    } catch (error) {
      console.error('上传失败:', error);
      throw error;
    }
  }
}

async function getUploadSignature(fileName) {
  const response = await fetch('/upload/signature?fileName=' + fileName, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + getToken(),
    },
  });

  const data = await response.json();
  return data.data;
}
```

---

## 🔧 完整的创建商品流程

```javascript
/**
 * 完整的创建商品流程
 */
async function createProductWithImage() {
  try {
    // 1. 选择并上传图片
    const fileInput = document.getElementById('productImage');
    const file = fileInput.files[0];

    if (!file) {
      alert('请选择图片');
      return;
    }

    // 2. 上传图片到OSS
    const imageUrl = await uploadToOSS(file);
    console.log('图片上传成功:', imageUrl);

    // 3. 创建商品（包含图片URL）
    const productData = {
      merchantId: 1,
      name: 'iPhone 15 Pro Max',
      brand: 'Apple',
      imageUrl: imageUrl,  // 使用OSS返回的URL
      description: '全新iPhone 15 Pro Max 256G',
      category: '手机数码',
      initialPrice: 7999.00,
      stock: 10
    };

    // 4. 提交商品信息
    const response = await fetch('/products', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + getToken(),
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(productData)
    });

    const result = await response.json();

    if (result.code === 200) {
      console.log('商品创建成功:', result.data);
      alert('商品创建成功！');
    } else {
      throw new Error(result.message);
    }

  } catch (error) {
    console.error('创建商品失败:', error);
    alert('操作失败: ' + error.message);
  }
}

/**
 * 上传到OSS的核心函数
 */
async function uploadToOSS(file) {
  // 1. 获取签名
  const signature = await getUploadSignature(file.name);

  // 2. 创建FormData
  const formData = new FormData();
  formData.append('file', file);
  formData.append('OSSAccessKeyId', signature.accessKeyId);
  formData.append('policy', signature.policy);
  formData.append('signature', signature.signature);
  formData.append('key', signature.uploadDir + file.name);
  formData.append('Content-Type', file.type);

  // 3. 上传到OSS
  const uploadUrl = `https://${signature.bucket}.${signature.region}.aliyuncs.com`;
  const response = await fetch(uploadUrl, {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`上传失败: ${errorText}`);
  }

  // 4. 返回图片URL
  return `${uploadUrl}/${signature.uploadDir}${file.name}`;
}

/**
 * 获取上传签名
 */
async function getUploadSignature(fileName) {
  const response = await fetch('/upload/signature?fileName=' + encodeURIComponent(fileName), {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + getToken(),
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    throw new Error('获取签名失败');
  }

  const data = await response.json();
  return data.data;
}

// 获取本地存储的token
function getToken() {
  return localStorage.getItem('token') || '';
}
```

---

## 🎨 高级功能

### 1. 批量上传

```javascript
/**
 * 批量上传多张图片
 */
async function uploadMultipleImages(files) {
  const imageUrls = [];

  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    try {
      console.log(`正在上传第 ${i + 1}/${files.length} 张图片...`);

      const imageUrl = await uploadToOSS(file);
      imageUrls.push(imageUrl);

      console.log(`第 ${i + 1} 张图片上传成功: ${imageUrl}`);

    } catch (error) {
      console.error(`第 ${i + 1} 张图片上传失败:`, error);
      // 继续上传下一张
    }
  }

  console.log(`批量上传完成，成功: ${imageUrls.length}/${files.length}`);
  return imageUrls;
}

// 使用示例
const fileInput = document.getElementById('productImages');
const files = Array.from(fileInput.files);
const imageUrls = await uploadMultipleImages(files);
```

### 2. 上传进度显示

```javascript
/**
 * 带进度的上传
 */
async function uploadWithProgress(file, onProgress) {
  const signature = await getUploadSignature(file.name);

  const formData = new FormData();
  formData.append('file', file);
  formData.append('OSSAccessKeyId', signature.accessKeyId);
  formData.append('policy', signature.policy);
  formData.append('signature', signature.signature);
  formData.append('key', signature.uploadDir + file.name);
  formData.append('Content-Type', file.type);

  const uploadUrl = `https://${signature.bucket}.${signature.region}.aliyuncs.com`;

  // 使用XMLHttpRequest获取上传进度
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable) {
        const percentComplete = Math.round((e.loaded / e.total) * 100);
        onProgress(percentComplete);
      }
    });

    xhr.addEventListener('load', () => {
      if (xhr.status === 200) {
        const imageUrl = `${uploadUrl}/${signature.uploadDir}${file.name}`;
        resolve(imageUrl);
      } else {
        reject(new Error('上传失败'));
      }
    });

    xhr.addEventListener('error', () => reject(new Error('网络错误')));
    xhr.open('POST', uploadUrl);
    xhr.send(formData);
  });
}

// 使用示例
uploadWithProgress(file, (progress) => {
  console.log(`上传进度: ${progress}%`);
  document.getElementById('progress').style.width = progress + '%';
});
```

### 3. 图片压缩后上传

```javascript
/**
 * 压缩图片后再上传
 */
async function compressAndUpload(file, quality = 0.8) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        // 创建canvas进行压缩
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');

        // 设置压缩后的尺寸（保持宽高比）
        const maxWidth = 1920;
        const maxHeight = 1080;
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > maxWidth) {
            height *= maxWidth / width;
            width = maxWidth;
          }
        } else {
          if (height > maxHeight) {
            width *= maxHeight / height;
            height = maxHeight;
          }
        }

        canvas.width = width;
        canvas.height = height;
        ctx.drawImage(img, 0, 0, width, height);

        // 压缩并上传
        canvas.toBlob(async (blob) => {
          const compressedFile = new File([blob], file.name, {
            type: 'image/jpeg',
            lastModified: Date.now()
          });

          try {
            const imageUrl = await uploadToOSS(compressedFile);
            resolve(imageUrl);
          } catch (error) {
            reject(error);
          }
        }, 'image/jpeg', quality);
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  });
}
```

---

## 📊 对比其他方案

| 方案 | 后端中转 | STS临时凭证 | 签名直传（推荐） |
|------|---------|-------------|------------------|
| **后端参与** | 完全参与 | 只提供凭证 | 只提供签名 |
| **服务器带宽** | ❌ 占用 | ✅ 不占用 | ✅ 不占用 |
| **实现复杂度** | 简单 | 中等 | 简单 |
| **安全性** | 高 | 很高 | 高 |
| **前端控制** | 低 | 中 | 高 |
| **适用场景** | 小文件 | 大文件 | 所有场景 |

---

## ⚠️ 注意事项

### 1. 文件路径
签名中包含了上传路径限制：
```javascript
// 前端必须使用后端返回的uploadDir
const objectKey = signature.uploadDir + fileName;
```

### 2. 签名过期
签名有1小时有效期，过期后需要重新获取：
```javascript
try {
  await uploadToOSS(file);
} catch (error) {
  if (error.message.includes('签名过期')) {
    // 重新获取签名
    const newSignature = await getUploadSignature(file.name);
    // 重试上传
  }
}
```

### 3. 跨域配置
确保OSS Bucket已配置CORS：
```json
{
  "AllowedOrigins": ["*"],
  "AllowedMethods": ["GET", "POST", "PUT", "DELETE"],
  "AllowedHeaders": ["*"],
  "ExposeHeaders": ["ETag"]
}
```

---

## 🚀 性能优化

### 1. CDN加速
```javascript
// 使用CDN域名返回给前端
const cdnUrl = imageUrl.replace(
  `https://${signature.bucket}.${signature.region}.aliyuncs.com`,
  'https://cdn.yourdomain.com'
);
```

### 2. 图片懒加载
```html
<img data-src={imageUrl} loading="lazy" alt="商品图片" />
<script>
  // Intersection Observer实现懒加载
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const img = entry.target;
        img.src = img.dataset.src;
        observer.unobserve(img);
      }
    });
  });

  document.querySelectorAll('img[data-src]').forEach(img => {
    observer.observe(img);
  });
</script>
```

---

这个方案完全不需要后端做中转，是最优雅、最高效的实现方式！