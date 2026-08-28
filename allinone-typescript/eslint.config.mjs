import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import parserVue from 'vue-eslint-parser'
import tseslint from 'typescript-eslint'
import parserTs from '@typescript-eslint/parser'

export default tseslint.config(
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: parserTs,
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: {
          jsx: true
        }
      },
      globals: {
        browser: true,
        es2022: true,
        node: true,
        vite: true
      }
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      'no-console': 'warn',
      'no-debugger': 'warn',
      'no-undef': 'off'
    }
  },
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: parserVue,
      parserOptions: {
        parser: parserTs,
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: {
          jsx: true
        },
        extraFileExtensions: ['.vue']
      },
      globals: {
        browser: true,
        es2022: true,
        node: true,
        vite: true,
        App: true,
        computed: true,
        createApp: true,
        onMounted: true,
        nextTick: true,
        ref: true,
        reactive: true,
        watch: true,
        computed: true,
        defineProps: true,
        defineEmits: true,
        defineExpose: true,
        useRoute: true,
        useRouter: true,
        getCurrentInstance: true,
        ElMessage: true,
        ElMessageBox: true
      }
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-unused-vars': 'error',
      'vue/no-unused-components': 'warn',
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      'no-console': 'warn',
      'no-debugger': 'warn',
      'no-undef': 'off'
    }
  },
  {
    files: ['**/*.vue'],
    rules: {
      'vue/require-default-prop': 'off',
      'vue/require-prop-types': 'off'
    }
  },
  {
    ignores: ['node_modules', 'dist', '*.config.*', 'shims-vue.d.ts', 'auto-imports.d.ts']
  }
)