import { Alert, Layout, Menu, Skeleton } from 'antd';
import React, { useMemo, useState } from 'react';
import { useJsonRpc } from '../../hooks/useJsonRpc';
import Command from '../Command/Command';
import Home from '../Home/Home';
import openRpcService from './openrpc.service';

const { Header, Content, Footer, Sider } = Layout;

const JSONRPC_OPENRPC_REQUEST = {
    jsonrpc: '2.0',
    method: 'openrpc',
};
export default () => {
    const [loading, error, data] = useJsonRpc(JSONRPC_OPENRPC_REQUEST);
    const [selectedMethod, setSelectedMethod] = useState();
    const { menuItems, indexedMethods, indexedSchemas } = useMemo(() => openRpcService(data), [data]);

    if (!loading && error) {
        return (
            <Alert
                type='error'
                message='Cannot load OpenRPC specification.'
            />
        );
    }

    const isHome = !selectedMethod || selectedMethod === 'home';
    return (
        <Layout style={{ minHeight: '100vh' }}>
            <Sider>
                <Skeleton active loading={loading} />
                {!loading && (
                    <Menu
                        items={menuItems}
                        mode='inline'
                        theme='dark'
                        selectedKeys={selectedMethod}
                        onSelect={e => setSelectedMethod(e.key)}
                        onDeselect={() => setSelectedMethod(undefined)}
                    />
                )}
            </Sider>
            <Layout>
                <Header className="site-layout-sub-header-background" style={{ padding: 0 }} />
                <Content>
                    <Skeleton active loading={loading} />
                    {!loading && !isHome && (
                        <div className="site-layout-background" style={{ margin: '1rem' }}>
                            <Command
                                method={indexedMethods[selectedMethod]}
                                schemas={indexedSchemas}
                            />
                        </div>
                    )}
                    {!loading && isHome && (
                        <Home />
                    )}
                </Content>
                <Footer style={{ textAlign: 'center' }}>Yupiik ©2022</Footer>
            </Layout>
        </Layout>
    );
};
