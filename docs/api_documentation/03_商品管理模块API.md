# 商品管理模块API文档

## 📋 模块说明

商品管理模块负责拍卖商品的信息管理，包括商品创建、查询等功能。

**🔗 基础路径**: `/api/products`

---

## ➕ 1. 创建商品

### 接口信息

- **接口地址**: `POST /api/products`
- **接口说明**: 创建新的拍卖商品
- **权限要求**: 需要登录，需要商家或管理员权限

### 请求参数

```json
{
  "name": "iPhone 15 Pro Max 256GB",
  "imageUrl": "https://example.com/iphone.jpg",
  "description": "全新未拆封，正品行货，支持验机",
  "category": "手机数码"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 | 验证规则 |
|--------|------|------|------|----------|
| name | String | ✅ | 商品名称 | 最多200字符 |
| imageUrl | String | ❌ | 商品图片URL | 有效的URL地址 |
| description | String | ❌ | 商品描述 | 详细的商品说明 |
| category | String | ❌ | 商品分类 | 最多50字符 |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "iPhone 15 Pro Max 256GB",
    "imageUrl": "https://example.com/iphone.jpg",
    "description": "全新未拆封，正品行货，支持验机",
    "category": "手机数码",
    "createdAt": "2026-05-31T10:00:00"
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "商品名称不能为空",
  "timestamp": 1717056000000
}
```

### 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查必填字段和格式 |
| 401 | 未登录 | 跳转到登录页面 |
| 403 | 无权限 | 提示用户需要商家权限 |

### 前端调用示例

```javascript
async function createProduct(formData) {
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch('http://localhost:8080/api/products', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      },
      body: JSON.stringify(formData)
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('商品创建成功:', result.data);
      return result.data;
    } else {
      console.error('商品创建失败:', result.message);
      showError(result.message);
    }
  } catch (error) {
    console.error('创建商品请求失败:', error);
    showError('网络错误，请稍后重试');
  }
}

// 使用示例
createProduct({
  name: 'iPhone 15 Pro Max',
  imageUrl: 'https://example.com/iphone.jpg',
  description: '全新未拆封',
  category: '手机数码'
});
```

---

## 🔍 2. 查询商品详情

### 接口信息

- **接口地址**: `GET /api/products/{id}`
- **接口说明**: 根据商品ID查询商品详细信息
- **权限要求**: 无需登录

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | ✅ | 商品ID |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "iPhone 15 Pro Max 256GB",
    "imageUrl": "https://example.com/iphone.jpg",
    "description": "全新未拆封，正品行货，支持验机",
    "category": "手机数码",
    "createdAt": "2026-05-31T10:00:00"
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 404,
  "message": "商品不存在",
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getProductDetail(productId) {
  try {
    const response = await fetch(`http://localhost:8080/api/products/${productId}`);
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('商品详情:', result.data);
      return result.data;
    } else {
      console.error('查询商品失败:', result.message);
      showError(result.message);
    }
  } catch (error) {
    console.error('查询商品请求失败:', error);
    showError('网络错误，请稍后重试');
  }
}

// React组件示例
function ProductDetail({ productId }) {
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    getProductDetail(productId).then(data => {
      setProduct(data);
      setLoading(false);
    });
  }, [productId]);
  
  if (loading) return <div>加载中...</div>;
  if (!product) return <div>商品不存在</div>;
  
  return (
    <div>
      <h1>{product.name}</h1>
      <img src={product.imageUrl} alt={product.name} />
      <p>{product.description}</p>
      <span>分类: {product.category}</span>
    </div>
  );
}
```

---

## 📋 3. 查询所有商品

### 接口信息

- **接口地址**: `GET /api/products`
- **接口说明**: 查询系统中所有商品列表
- **权限要求**: 无需登录

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "iPhone 15 Pro Max 256GB",
      "imageUrl": "https://example.com/iphone.jpg",
      "description": "全新未拆封，正品行货",
      "category": "手机数码",
      "createdAt": "2026-05-31T10:00:00"
    },
    {
      "id": 2,
      "name": "MacBook Pro 14英寸",
      "imageUrl": "https://example.com/macbook.jpg",
      "description": "M3芯片，16GB内存，512GB存储",
      "category": "电脑办公",
      "createdAt": "2026-05-31T11:00:00"
    }
  ],
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getAllProducts() {
  try {
    const response = await fetch('http://localhost:8080/api/products');
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('商品列表:', result.data);
      return result.data;
    } else {
      console.error('查询商品列表失败:', result.message);
      showError(result.message);
    }
  } catch (error) {
    console.error('查询商品列表请求失败:', error);
    showError('网络错误，请稍后重试');
  }
}

// React组件示例
function ProductList() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    getAllProducts().then(data => {
      setProducts(data);
      setLoading(false);
    });
  }, []);
  
  if (loading) return <div>加载中...</div>;
  
  return (
    <div className="product-grid">
      {products.map(product => (
        <div key={product.id} className="product-card">
          <img src={product.imageUrl} alt={product.name} />
          <h3>{product.name}</h3>
          <p>{product.description}</p>
          <span>{product.category}</span>
          <Link to={`/products/${product.id}`}>查看详情</Link>
        </div>
      ))}
    </div>
  );
}
```

---

## 📝 完整商品管理示例

### 商品创建表单

```javascript
// ProductForm.js
import { useState } from 'react';

function ProductForm() {
  const [formData, setFormData] = useState({
    name: '',
    imageUrl: '',
    description: '',
    category: ''
  });
  
  const [submitting, setSubmitting] = useState(false);
  
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // 基本验证
    if (!formData.name.trim()) {
      showError('商品名称不能为空');
      return;
    }
    
    if (formData.name.length > 200) {
      showError('商品名称不能超过200字符');
      return;
    }
    
    setSubmitting(true);
    
    try {
      const result = await createProduct(formData);
      showSuccess('商品创建成功！');
      
      // 重置表单
      setFormData({
        name: '',
        imageUrl: '',
        description: '',
        category: ''
      });
      
      // 可选：跳转到商品详情页
      // window.location.href = `/products/${result.id}`;
      
    } catch (error) {
      showError('创建商品失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <div className="form-group">
        <label>商品名称 *</label>
        <input
          type="text"
          name="name"
          value={formData.name}
          onChange={handleChange}
          maxLength={200}
          placeholder="请输入商品名称"
          required
        />
        <small>最多200字符</small>
      </div>
      
      <div className="form-group">
        <label>商品图片URL</label>
        <input
          type="url"
          name="imageUrl"
          value={formData.imageUrl}
          onChange={handleChange}
          placeholder="请输入商品图片URL"
        />
      </div>
      
      <div className="form-group">
        <label>商品描述</label>
        <textarea
          name="description"
          value={formData.description}
          onChange={handleChange}
          placeholder="请输入商品描述"
          rows="4"
        />
      </div>
      
      <div className="form-group">
        <label>商品分类</label>
        <input
          type="text"
          name="category"
          value={formData.category}
          onChange={handleChange}
          maxLength={50}
          placeholder="请输入商品分类"
        />
        <small>最多50字符</small>
      </div>
      
      <button 
        type="submit" 
        disabled={submitting}
        className="btn btn-primary"
      >
        {submitting ? '创建中...' : '创建商品'}
      </button>
    </form>
  );
}
```

### 商品列表组件

```javascript
// ProductList.js
import { useState, useEffect } from 'react';

function ProductList() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    loadProducts();
  }, []);
  
  const loadProducts = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const data = await getAllProducts();
      setProducts(data);
    } catch (err) {
      setError('加载商品列表失败');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };
  
  if (loading) {
    return (
      <div className="text-center py-8">
        <div className="spinner"></div>
        <p>加载中...</p>
      </div>
    );
  }
  
  if (error) {
    return (
      <div className="alert alert-error">
        {error}
        <button onClick={loadProducts} className="btn btn-secondary ml-2">
          重试
        </button>
      </div>
    );
  }
  
  if (products.length === 0) {
    return (
      <div className="text-center py-8">
        <p>暂无商品</p>
      </div>
    );
  }
  
  return (
    <div className="product-grid">
      {products.map(product => (
        <ProductCard 
          key={product.id} 
          product={product}
          onClick={() => window.location.href = `/products/${product.id}`}
        />
      ))}
    </div>
  );
}

function ProductCard({ product, onClick }) {
  return (
    <div className="product-card" onClick={onClick}>
      <div className="product-image">
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} />
        ) : (
          <div className="no-image">暂无图片</div>
        )}
      </div>
      <div className="product-info">
        <h3>{product.name}</h3>
        {product.category && (
          <span className="badge">{product.category}</span>
        )}
        {product.description && (
          <p className="description">
            {product.description.substring(0, 100)}
            {product.description.length > 100 ? '...' : ''}
          </p>
        )}
      </div>
    </div>
  );
}
```

---

## 🖼️ 图片上传功能

虽然商品接口支持直接传图片URL，但实际开发中通常需要先上传图片。这里提供一个通用的图片上传解决方案：

```javascript
// utils/imageUpload.js
async function uploadImage(file) {
  // 验证文件
  if (!file) {
    throw new Error('请选择文件');
  }
  
  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
  if (!allowedTypes.includes(file.type)) {
    throw new Error('只支持JPG、PNG、GIF格式的图片');
  }
  
  // 验证文件大小（5MB）
  const maxSize = 5 * 1024 * 1024;
  if (file.size > maxSize) {
    throw new Error('图片大小不能超过5MB');
  }
  
  // 创建FormData
  const formData = new FormData();
  formData.append('file', file);
  
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch('http://localhost:8080/api/products/images/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      },
      body: formData
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('图片上传成功:', result.data.imageUrl);
      return result.data.imageUrl;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('图片上传失败:', error);
    throw error;
  }
}

// 图片上传组件
function ImageUploader({ onImageUploaded }) {
  const [uploading, setUploading] = useState(false);
  const [preview, setPreview] = useState(null);
  
  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    // 显示预览
    setPreview(URL.createObjectURL(file));
    
    // 上传图片
    setUploading(true);
    try {
      const imageUrl = await uploadImage(file);
      onImageUploaded(imageUrl);
      showSuccess('图片上传成功');
    } catch (error) {
      showError(error.message);
      setPreview(null);
    } finally {
      setUploading(false);
    }
  };
  
  return (
    <div className="image-uploader">
      <div className="preview-area">
        {preview ? (
          <img src={preview} alt="预览" />
        ) : (
          <div className="placeholder">暂无图片</div>
        )}
      </div>
      
      <input
        type="file"
        onChange={handleFileChange}
        accept="image/jpeg,image/png,image/gif"
        disabled={uploading}
      />
      
      {uploading && <div className="uploading">上传中...</div>}
    </div>
  );
}
```

---

## 🎨 商品卡片样式建议

```css
/* 商品卡片样式 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding: 20px;
}

.product-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.product-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.product-image {
  width: 100%;
  height: 200px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: #999;
  font-size: 14px;
}

.product-info {
  padding: 16px;
}

.product-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  background: #e3f2fd;
  color: #1976d2;
  border-radius: 4px;
  font-size: 12px;
  margin-bottom: 8px;
}

.description {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}
```

---

## ⚠️ 注意事项

### 1. 权限控制

- 创建商品接口需要商家或管理员权限
- 建议在前端也进行权限检查，无权限时隐藏创建按钮

### 2. 数据验证

- 前端应该验证必填字段
- 商品名称长度限制为200字符
- 分类名称长度限制为50字符

### 3. 图片处理

- 商品图片URL必须是有效的URL
- 建议使用图片上传功能而不是直接输入URL
- 支持的图片格式：JPG、PNG、GIF
- 图片大小限制：5MB

### 4. 错误处理

- 404错误表示商品不存在
- 403错误表示无权限创建商品
- 400错误表示参数验证失败

---

## 📞 技术支持

如有接口问题，请联系后端开发团队。

**最后更新**: 2026-05-31
**文档版本**: v1.0.0
