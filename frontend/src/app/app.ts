import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ZardToastComponent } from '@/shared/components/toast';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ZardToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
