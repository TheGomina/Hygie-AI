import { redirect } from 'next/navigation';

export default function Home() {
  redirect('/bmp');
  return null;
}