import { Routes } from '@angular/router';
import { DashboardComponent } from '@/features/dashboard/dashboard.component';
import { LoginComponent } from '@/features/auth/login/login.component';
import { RegisterComponent } from '@/features/auth/register/register.component';
import { authGuard } from '@/features/auth/auth.guard';
import { VideoComponent } from './shared/components/video/video.component';
import { ExploreComponent } from '@/features/explore/explore.component';
import { ChannelsComponent } from '@/features/channels/channels.component';
import { UploadPageComponent } from '@/features/uploads/upload-page.component';
import { VideoManageComponent } from '@/features/videos/video-manage/video-manage.component';
import { Studio } from './features/studio/studio.component';

export const routes: Routes = [
  {
    path: '',
    component: DashboardComponent,
    canActivate: [authGuard],
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'register',
    component: RegisterComponent,
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
  },
  {
    path: 'explore',
    component: ExploreComponent,
  },
  {
    path: 'channels',
    component: ChannelsComponent,
  },
  {
    path: 'upload',
    component: UploadPageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'videos/:id/manage',
    component: VideoManageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'videos/:id',
    component: VideoComponent,
  },
  {
    path: 'studio',
    component: Studio,
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: '/',
  },
];
