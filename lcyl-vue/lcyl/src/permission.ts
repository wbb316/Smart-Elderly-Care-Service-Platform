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
    const perms: string[] = record.permissions || record.meta?.permissions
    if (perms && perms.length > 0) {
      const userPerms = useUserStore().permissions
      if (!userPerms || userPerms.length === 0) {
        return false
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
