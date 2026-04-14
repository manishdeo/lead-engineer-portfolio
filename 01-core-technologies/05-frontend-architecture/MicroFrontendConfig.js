/**
 * Webpack 5 Module Federation Configuration Example
 * Demonstrates how to configure a Host and a Remote application in a Micro-Frontend architecture.
 */

const HtmlWebpackPlugin = require('html-webpack-plugin');
const ModuleFederationPlugin = require('webpack/lib/container/ModuleFederationPlugin');
const deps = require('./package.json').dependencies || {};

module.exports = {
  // Remote Application Configuration (e.g., Header App)
  remoteConfig: {
    mode: 'development',
    devServer: { port: 3001 },
    plugins: [
      new ModuleFederationPlugin({
        name: 'headerApp', // Name of the remote
        filename: 'remoteEntry.js', // Entry file for the host to consume
        exposes: {
          './Header': './src/components/Header', // Expose the Header component
        },
        shared: {
          ...deps,
          react: { singleton: true, requiredVersion: deps.react || '^18.0.0' },
          'react-dom': { singleton: true, requiredVersion: deps['react-dom'] || '^18.0.0' },
        },
      }),
      new HtmlWebpackPlugin({ template: './public/index.html' }),
    ],
  },

  // Host Application Configuration (e.g., Main App shell)
  hostConfig: {
    mode: 'development',
    devServer: { port: 3000 },
    plugins: [
      new ModuleFederationPlugin({
        name: 'hostApp',
        remotes: {
          // Consume the Header app from the remote server
          headerApp: 'headerApp@http://localhost:3001/remoteEntry.js',
        },
        shared: {
          ...deps,
          react: { singleton: true, requiredVersion: deps.react || '^18.0.0' },
          'react-dom': { singleton: true, requiredVersion: deps['react-dom'] || '^18.0.0' },
        },
      }),
      new HtmlWebpackPlugin({ template: './public/index.html' }),
    ],
  }
};
