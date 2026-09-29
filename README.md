# .github

`ice-wocker` 账号下所有仓库共用的社区健康文件。

## 这个仓库存在的意义

11 个仓库里的 `CONTRIBUTING.md` 和 `SECURITY.md` 原本是**逐份复制**的
（md5 完全相同）。问题是改一处要改 11 处，漏一个就出现两套规则。

GitHub 支持 [默认社区健康文件机制](https://docs.github.com/communities/setting-up-your-project-for-healthy-contributions/creating-a-default-community-health-file)：
仓库如果**自己**没有 `CONTRIBUTING.md` / `SECURITY.md` / Issue 模板，
就自动使用本仓库里的对应文件。

所以正确做法是：规则放这里改一次，各项目仓库里不要放副本。

## 包含什么

| 文件 | 作用 |
| --- | --- |
| `CONTRIBUTING.md` | 贡献指南 + 四条项目底线（不引依赖、离线优先、体积敏感、隐私默认安全） |
| `SECURITY.md` | 私密漏洞报告渠道 |
| `.github/ISSUE_TEMPLATE/bug_report.yml` | 结构化 Bug 表单 |
| `.github/ISSUE_TEMPLATE/feature_request.yml` | 功能建议表单（含「对依赖/体积/离线的影响」一问） |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR 自查清单 |

## 哪些仓库不使用

- `ice-wocker`（Profile 仓库）—— 纯 README，没有协作面
- 个别项目若需要**特殊规则**，可在自己仓库里放一份覆盖
