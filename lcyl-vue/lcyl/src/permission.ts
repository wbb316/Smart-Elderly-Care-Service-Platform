import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path: string): boolean => {
  return whiteList.some((pattern: string) => isPathMatch(pattern, path))
}

function hasRoutePermission(route: any): boolean {
  const matched = route.matched || []
  for (const record of matched) {
    // 权限必须声明在 meta.permissions 里。
    // 原因：Vue Router 4 会丢弃路由配置顶层的自定义字段，
    //      匹配到的 record 上 record.permissions 恒为 undefined，
    //      之前写在顶层的 permissions 从未生效过。
    const perms: string[] = record.meta?.permissions
    if (perms && perms.length > 0) {
      const userPerms: string[] = useUserStore().permissions
      if (!userPerms || userPerms.length === 0) {
        return false
      }
      // 超级管理员拥有 "*:*:*"；原实现用 includes 精确匹配，会把超管挡在门外
      if (userPerms.includes('*:*:*')) {
        continue
      }
      if (!perms.some((p: string) => userPerms.includes(p))) {
        return false
      }
    }
  }
  return true
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title as string)
    if (to.path === '/login') {
      next({ path: '/' })
    } else if (isWhiteList(to.path)) {
      next()
    } else {
      if (useUserStore().roles.length === 0) {
        isRelogin.show = true
        useUserStore().getInfo().then(() => {
          isRelogin.show = false
          usePermissionStore().generateRoutes().then((accessRoutes: any[]) => {
            accessRoutes.forEach((route: any) => {
              if (!isHttp(route.path)) {
                router.addRoute(route)
              }
            })
            if (!hasRoutePermission(to)) {
              ElMessage.error('没有访问权限')
              next({ path: '/401' })
              return
            }
            next({ ...to, replace: true })
          })
        }).catch((err: any) => {
          useUserStore().logOut().then(() => {
            ElMessage.error(err as string)
            next({ path: '/' })
          }).catch(() => {
            next({ path: '/' })
          })
        })
      } else {
        if (!hasRoutePermission(to)) {
          ElMessage.error('没有访问权限')
          next({ path: '/401' })
          return
        }
        next()
      }
    }
  } else {
    if (isWhiteList(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`)
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
