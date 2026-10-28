const nextJest = require("next/jest");

const createJestConfig = nextJest({ dir: "./" });

const customJestConfig = {
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/jest.setup.js"],
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/$1",
    "^msw/node$": "<rootDir>/node_modules/msw/lib/node/index.js",
    "^@mswjs/interceptors/(ClientRequest|fetch|XMLHttpRequest)$": "<rootDir>/node_modules/@mswjs/interceptors/lib/node/interceptors/$1/index.js",
    "^@mswjs/interceptors/WebSocket$": "<rootDir>/node_modules/@mswjs/interceptors/lib/browser/interceptors/WebSocket/index.js",
    "^@mswjs/interceptors/RemoteHttpInterceptor$": "<rootDir>/node_modules/@mswjs/interceptors/lib/node/RemoteHttpInterceptor.js",
  },
  modulePathIgnorePatterns: ["<rootDir>/.next/"],
  testPathIgnorePatterns: ["<rootDir>/node_modules/", "<rootDir>/.next/"],
  transformIgnorePatterns: ["/node_modules/(?!msw|@mswjs|until-async)/"],
  collectCoverageFrom: [
    "app/**/*.{js,jsx}",
    "components/**/*.{js,jsx}",
    "hooks/**/*.{js,jsx}",
    "lib/**/*.{js,jsx}",
    "services/**/*.{js,jsx}",
    "stores/**/*.{js,jsx}",
    "!app/**/layout.jsx",
    "!app/**/page.jsx",
    "!**/*.test.{js,jsx}",
  ],
};

module.exports = createJestConfig(customJestConfig);
