# Admin Console Engineering Guide

React 18 / TypeScript / Vite / Ant Design v5. Proxies `/api`, `/admin`, `/ws` to backend at `:8080`.

## File structure

```
src/
  pages/          One file per page (Dashboard, Consultations, Purchases, LlmConfig)
  components/     Shared layout (Layout.tsx — sidebar + header)
  api/
    client.ts     All axios calls — one exported function per endpoint
    types.ts      TypeScript interfaces matching backend DTOs
  context/
    LanguageContext.tsx   EN/ZH translations + useLang() hook
```

## Recipe: add a new page

**Example: adding a "Health Tips" management page.**

1. **Add API types** in `src/api/types.ts`:
   ```ts
   export interface HealthTipDto {
     id: number;
     category: string;
     content: string;
   }
   ```

2. **Add API call** in `src/api/client.ts`:
   ```ts
   export const getHealthTips = () =>
     api.get<HealthTipDto[]>('/api/health-tips').then(r => r.data);
   ```

3. **Add translations** in `src/context/LanguageContext.tsx` — both EN and ZH keys required:
   ```ts
   // EN block:
   healthTips: 'Health Tips',
   category:   'Category',

   // ZH block:
   healthTips: '健康贴士',
   category:   '分类',
   ```

4. **Create the page** `src/pages/HealthTips.tsx` — follow this skeleton:
   ```tsx
   import React, { useEffect, useState } from 'react';
   import { Card, Table, Typography } from 'antd';
   import { getHealthTips } from '../api/client';
   import type { HealthTipDto } from '../api/types';
   import { useLang } from '../context/LanguageContext';

   const { Title } = Typography;

   const HealthTips: React.FC = () => {
     const [data, setData] = useState<HealthTipDto[]>([]);
     const [loading, setLoading] = useState(true);
     const { t } = useLang();

     useEffect(() => {
       getHealthTips()
         .then(setData)
         .catch(() => {})
         .finally(() => setLoading(false));
     }, []);

     const columns = [
       { title: t('category'), dataIndex: 'category', key: 'category' },
       { title: 'Content',     dataIndex: 'content',  key: 'content'  },
     ];

     return (
       <Card title={<Title level={5} style={{ margin: 0 }}>{t('healthTips')}</Title>}>
         <Table dataSource={data} columns={columns} rowKey="id"
                loading={loading} pagination={{ pageSize: 10 }} size="middle" />
       </Card>
     );
   };

   export default HealthTips;
   ```

5. **Register route + menu item** in `src/App.tsx` and `src/components/Layout.tsx`:

   `App.tsx` — add inside `<Routes>`:
   ```tsx
   import HealthTips from './pages/HealthTips';
   <Route path="/health-tips" element={<HealthTips />} />
   ```

   `Layout.tsx` — add to `menuItems` array:
   ```tsx
   import { BulbOutlined } from '@ant-design/icons';
   { key: '/health-tips', icon: <BulbOutlined />, label: t('healthTips') },
   ```

## Important conventions

- **Never inline API calls** in page components — keep them in `api/client.ts`
- **Every string shown to the user** must be in `LanguageContext.tsx` (both EN + ZH)
- **Error handling**: catch fetch errors silently (`catch(() => {})`) on read; `message.error()` on writes
- **Loading state**: always start with `loading=true`, set false in `.finally()`
- **Card + Title pattern**: all pages wrapped in `<Card title={<Title level={5}>...}>` for visual consistency

## Vite proxy

Routes proxied in `vite.config.ts`:
- `/api/*` → `http://localhost:8080`
- `/admin/*` → `http://localhost:8080`
- `/ws` → `http://localhost:8080` (WebSocket)

Do not use absolute backend URLs in `client.ts`.

## Key known issue

`vite.config.ts` must keep `optimizeDeps.esbuildOptions.define: { global: 'globalThis' }` — removing it causes a blank page (sockjs-client uses Node.js `global` which doesn't exist in browsers).
